//! Full TDLib-backed implementation of the [`crate::transport::Transport`] trait.
//!
//! Spawns a Tokio background worker that drives:
//! 1. `tdlib_rs::receive()` loop for consuming incoming updates from Telegram servers.
//! 2. Automatic initialization of TDLib parameters (API ID, API Hash, database directory).
//! 3. Asynchronous execution of authorization actions (phone, code, password).
//! 4. Non-blocking messaging operations (sending, history retrieval, chat sync).
//!
//! Events from TDLib are mapped to [`crate::transport::CoreEvent`] and queued
//! into a thread-safe MPSC channel drained by the Slint UI thread via [`Transport::poll`].

use std::sync::mpsc::{channel, Receiver};
use std::sync::{Arc, Mutex};
use std::thread;

use crate::model::Message;
use crate::tdlib_backend::{map_auth_state, map_chat, map_message, map_update, AuthState, TdlibParameters};
use crate::transport::{CoreEvent, EventSink, Transport, TransportError};

/// Default API credentials provided for the client session.
pub const DEFAULT_API_ID: i32 = 27048025;
pub const DEFAULT_API_HASH: &str = "d4725bc976e1a1c784ffbb1a61748390";

enum TdlibCommand {
    SetPhoneNumber(String),
    CheckAuthCode(String),
    CheckPassword(String),
    RegisterUser { first_name: String, last_name: String },
    AddProxy { server: String, port: i32, secret: Option<String> },
    SendMessage { chat_id: i64, text: String },
    LoadChats { limit: i32 },
    LoadChatHistory { chat_id: i64, from_msg_id: i64, limit: i32 },
    MarkChatRead { chat_id: i64 },
    GetChat { chat_id: i64 },
    GetUser { user_id: i64 },
    Stop,
}

/// A fully-functional TDLib transport.
pub struct TdlibTransport {
    client_id: i32,
    params: TdlibParameters,
    cmd_tx: Option<tokio::sync::mpsc::UnboundedSender<TdlibCommand>>,
    event_rx: Receiver<CoreEvent>,
    worker_handle: Option<thread::JoinHandle<()>>,
    current_auth_state: Arc<Mutex<AuthState>>,
    my_user_id: Arc<Mutex<Option<i64>>>,
    last_error: Arc<Mutex<Option<String>>>,
}

impl TdlibTransport {
    /// Creates a new TDLib transport with the default Telegram API credentials.
    pub fn new_default() -> Self {
        let params = TdlibParameters {
            api_id: DEFAULT_API_ID,
            api_hash: DEFAULT_API_HASH.to_string(),
            ..TdlibParameters::default()
        };
        Self::with_parameters(params)
    }

    /// Creates a new TDLib transport with explicit parameters.
    pub fn with_parameters(params: TdlibParameters) -> Self {
        let (event_tx, event_rx) = channel();
        let _ = event_tx; // Will create inside start()

        Self {
            client_id: 0,
            params,
            cmd_tx: None,
            event_rx,
            worker_handle: None,
            current_auth_state: Arc::new(Mutex::new(AuthState::WaitingTdlibParameters)),
            my_user_id: Arc::new(Mutex::new(None)),
            last_error: Arc::new(Mutex::new(None)),
        }
    }

    /// Returns the current known authorization state.
    pub fn auth_state(&self) -> AuthState {
        self.current_auth_state.lock().unwrap().clone()
    }

    /// Takes the latest asynchronous authentication/initialization error.
    pub fn take_error(&self) -> Option<String> {
        self.last_error.lock().unwrap().take()
    }
}

impl Transport for TdlibTransport {
    fn name(&self) -> &'static str {
        "tdlib"
    }

    fn start(&mut self) -> Result<(), TransportError> {
        if self.worker_handle.is_some() {
            return Ok(());
        }

        let (event_tx, event_rx) = channel::<CoreEvent>();
        self.event_rx = event_rx;

        let (cmd_tx, mut cmd_rx) = tokio::sync::mpsc::unbounded_channel::<TdlibCommand>();
        self.cmd_tx = Some(cmd_tx);

        let client_id = tdlib_rs::create_client();
        self.client_id = client_id;

        let params = self.params.clone();
        let auth_state_arc = Arc::clone(&self.current_auth_state);
        let my_user_id_arc = Arc::clone(&self.my_user_id);
        let last_error = Arc::clone(&self.last_error);

        let handle = thread::spawn(move || {
            let rt = tokio::runtime::Builder::new_multi_thread()
                .worker_threads(2)
                .enable_all()
                .build()
                .expect("Failed to initialize Tokio runtime for TDLib worker");

            rt.block_on(async move {
                // Task 1: TDLib receive loop MUST be spawned first, because
                // any request sent via tdlib_rs waits on an internal oneshot channel
                // that is ONLY resolved when tdlib_rs::receive() processes the @extra response!
                let event_tx_clone = event_tx.clone();
                let auth_state_clone = Arc::clone(&auth_state_arc);
                let my_user_id_clone = Arc::clone(&my_user_id_arc);
                let params_for_init = params.clone();
                let init_error = Arc::clone(&last_error);

                let receive_task = tokio::spawn(async move {
                    loop {
                        // Receive next update from TDLib (blocking up to 2.0s internally in C++)
                        let opt_update = tokio::task::spawn_blocking(tdlib_rs::receive).await;
                        match opt_update {
                            Ok(Some((update, cid))) if cid == client_id => {
                                match &update {
                                    tdlib_rs::enums::Update::AuthorizationState(a) => {
                                        let new_auth = map_auth_state(&a.authorization_state);
                                        *auth_state_clone.lock().unwrap() = new_auth.clone();

                                        let _ = event_tx_clone.send(CoreEvent::AuthStateChanged {
                                            state: format!("{new_auth:?}"),
                                        });

                                        // Never await a TDLib request in this receive task. A request
                                        // response is delivered by this very receive loop, so awaiting
                                        // here deadlocks authorization before the phone screen appears.
                                        if let tdlib_rs::enums::AuthorizationState::WaitTdlibParameters = &a.authorization_state {
                                            let params = params_for_init.clone();
                                            let init_error = Arc::clone(&init_error);
                                            tokio::spawn(async move {
                                                let res = tdlib_rs::functions::set_tdlib_parameters(
                                                    params.use_test_dc,
                                                    params.database_directory,
                                                    params.files_directory,
                                                    String::new(),
                                                    params.use_file_database,
                                                    params.use_chat_info_database,
                                                    params.use_message_database,
                                                    false,
                                                    params.api_id,
                                                    params.api_hash,
                                                    params.system_language_code,
                                                    params.device_model,
                                                    params.system_version,
                                                    params.application_version,
                                                    client_id,
                                                ).await;
                                                if let Err(error) = res {
                                                    *init_error.lock().unwrap() = Some(error.message);
                                                }
                                            });
                                        }

                                        // When ready, fetch current user ID and trigger initial chat list load
                                        if let tdlib_rs::enums::AuthorizationState::Ready = &a.authorization_state {
                                            let my_user_id_ready = Arc::clone(&my_user_id_clone);
                                            tokio::spawn(async move {
                                                if let Ok(tdlib_rs::enums::User::User(u)) = tdlib_rs::functions::get_me(client_id).await {
                                                    *my_user_id_ready.lock().unwrap() = Some(u.id);
                                                }
                                                let _ = tdlib_rs::functions::load_chats(None, 50, client_id).await;
                                            });
                                        }
                                    }
                                    tdlib_rs::enums::Update::NewChat(c) => {
                                        if let Some(p) = &c.chat.photo {
                                            if !p.small.local.is_downloading_completed && p.small.id != 0 {
                                                let file_id = p.small.id;
                                                tokio::spawn(async move {
                                                    let _ = tdlib_rs::functions::download_file(file_id, 1, 0, 0, false, client_id).await;
                                                });
                                            }
                                        }
                                        let dialog = map_chat(&c.chat);
                                        let _ = event_tx_clone.send(CoreEvent::DialogUpserted(dialog));
                                        let _ = event_tx_clone.send(CoreEvent::DialogsChanged);
                                    }
                                    tdlib_rs::enums::Update::File(f) => {
                                        if f.file.local.is_downloading_completed && !f.file.local.path.is_empty() {
                                            let _ = event_tx_clone.send(CoreEvent::FileDownloaded {
                                                file_id: f.file.id,
                                                path: f.file.local.path.clone(),
                                            });
                                        }
                                    }
                                    _ => {
                                        let my_uid = *my_user_id_clone.lock().unwrap();
                                        if let Some(event) = map_update(&update, my_uid) {
                                            let _ = event_tx_clone.send(event);
                                        }
                                    }
                                }
                            }
                            Ok(_) => {}
                            Err(_) => break,
                        }
                    }
                });

                // TDLib requires at least one request to activate the update stream for a client.
                let _ = tdlib_rs::functions::set_log_verbosity_level(1, client_id).await;
                let _ = tdlib_rs::functions::get_authorization_state(client_id).await;

                // Task 2: Command processing loop
                while let Some(cmd) = cmd_rx.recv().await {
                    match cmd {
                        TdlibCommand::SetPhoneNumber(phone) => {
                            let result = tdlib_rs::functions::set_authentication_phone_number(
                                phone,
                                None,
                                client_id,
                            ).await;
                            *last_error.lock().unwrap() = result.err().map(|error| error.message);
                        }
                        TdlibCommand::CheckAuthCode(code) => {
                            let result = tdlib_rs::functions::check_authentication_code(
                                code,
                                client_id,
                            ).await;
                            *last_error.lock().unwrap() = result.err().map(|error| error.message);
                        }
                        TdlibCommand::CheckPassword(pwd) => {
                            let result = tdlib_rs::functions::check_authentication_password(
                                pwd,
                                client_id,
                            ).await;
                            *last_error.lock().unwrap() = result.err().map(|error| error.message);
                        }
                        TdlibCommand::RegisterUser { first_name, last_name } => {
                            let _ = tdlib_rs::functions::register_user(
                                first_name,
                                last_name,
                                false,
                                client_id,
                            ).await;
                        }
                        TdlibCommand::AddProxy { server, port, secret } => {
                            let proxy_type = match secret {
                                Some(sec) if !sec.is_empty() => tdlib_rs::enums::ProxyType::Mtproto(
                                    tdlib_rs::types::ProxyTypeMtproto { secret: sec }
                                ),
                                _ => tdlib_rs::enums::ProxyType::Socks5(
                                    tdlib_rs::types::ProxyTypeSocks5 { username: String::new(), password: String::new() }
                                ),
                            };
                            let proxy = tdlib_rs::types::Proxy {
                                server,
                                port,
                                r#type: proxy_type,
                            };
                            let _ = tdlib_rs::functions::add_proxy(
                                proxy,
                                true, // enable
                                client_id,
                            ).await;
                        }
                        TdlibCommand::SendMessage { chat_id, text } => {
                            let content = tdlib_rs::enums::InputMessageContent::InputMessageText(
                                tdlib_rs::types::InputMessageText {
                                    text: tdlib_rs::types::FormattedText {
                                        text,
                                        entities: Vec::new(),
                                    },
                                    link_preview_options: None,
                                    clear_draft: true,
                                }
                            );
                            let _ = tdlib_rs::functions::send_message(
                                chat_id,
                                None,
                                None,
                                None,
                                content,
                                client_id,
                            ).await;
                        }
                        TdlibCommand::LoadChats { limit } => {
                            let _ = tdlib_rs::functions::load_chats(None, limit, client_id).await;
                        }
                        TdlibCommand::LoadChatHistory { chat_id, from_msg_id, limit } => {
                            if let Ok(tdlib_rs::enums::Messages::Messages(msgs)) = tdlib_rs::functions::get_chat_history(
                                chat_id,
                                from_msg_id,
                                0,
                                limit,
                                false,
                                client_id,
                            ).await {
                                let my_uid = *my_user_id_arc.lock().unwrap();
                                let mut domain_messages: Vec<Message> = msgs.messages
                                    .iter()
                                    .flatten()
                                    .map(|m| map_message(m, my_uid))
                                    .collect();
                                domain_messages.reverse(); // Older first for display
                                let _ = event_tx.send(CoreEvent::MessagesLoaded {
                                    dialog_id: chat_id,
                                    messages: domain_messages,
                                });
                            }
                        }
                        TdlibCommand::MarkChatRead { chat_id } => {
                            if let Ok(tdlib_rs::enums::Chat::Chat(chat)) = tdlib_rs::functions::get_chat(chat_id, client_id).await {
                                if let Some(message) = chat.last_message {
                                    let _ = tdlib_rs::functions::view_messages(
                                        chat_id,
                                        vec![message.id],
                                        None,
                                        true,
                                        client_id,
                                    ).await;
                                }
                            }
                        }
                        TdlibCommand::GetChat { chat_id } => {
                            if let Ok(tdlib_rs::enums::Chat::Chat(chat)) = tdlib_rs::functions::get_chat(chat_id, client_id).await {
                                let dialog = map_chat(&chat);
                                let _ = event_tx.send(CoreEvent::DialogUpserted(dialog));
                            }
                        }
                        TdlibCommand::GetUser { user_id } => {
                            if let Ok(tdlib_rs::enums::User::User(user)) = tdlib_rs::functions::get_user(user_id, client_id).await {
                                if let Some(p) = &user.profile_photo {
                                    if !p.small.local.is_downloading_completed && p.small.id != 0 {
                                        let fid = p.small.id;
                                        tokio::spawn(async move {
                                            let _ = tdlib_rs::functions::download_file(fid, 1, 0, 0, false, client_id).await;
                                        });
                                    }
                                }
                            }
                        }
                        TdlibCommand::Stop => {
                            receive_task.abort();
                            break;
                        }
                    }
                }
            });
        });

        self.worker_handle = Some(handle);
        Ok(())
    }

    fn stop(&mut self) {
        if let Some(tx) = self.cmd_tx.take() {
            let _ = tx.send(TdlibCommand::Stop);
        }
        if let Some(handle) = self.worker_handle.take() {
            let _ = handle.join();
        }
    }

    fn poll(&mut self, sink: &mut dyn EventSink) -> Result<(), TransportError> {
        // Keep a burst of updates from monopolizing the Android main thread.
        for _ in 0..256 {
            let Ok(event) = self.event_rx.try_recv() else { break };
            sink.on_event(event);
        }
        Ok(())
    }

    fn send_message(&mut self, dialog_id: i64, body: &str) -> Result<(), TransportError> {
        let Some(tx) = &self.cmd_tx else {
            return Err(TransportError::NotConnected);
        };
        tx.send(TdlibCommand::SendMessage {
            chat_id: dialog_id,
            text: body.to_string(),
        }).map_err(|_| TransportError::NotConnected)
    }

    fn set_phone_number(&mut self, phone: &str) -> Result<(), TransportError> {
        let Some(tx) = &self.cmd_tx else {
            return Err(TransportError::NotConnected);
        };
        tx.send(TdlibCommand::SetPhoneNumber(phone.to_string()))
            .map_err(|_| TransportError::NotConnected)
    }

    fn check_auth_code(&mut self, code: &str) -> Result<(), TransportError> {
        let Some(tx) = &self.cmd_tx else {
            return Err(TransportError::NotConnected);
        };
        tx.send(TdlibCommand::CheckAuthCode(code.to_string()))
            .map_err(|_| TransportError::NotConnected)
    }

    fn check_password(&mut self, password: &str) -> Result<(), TransportError> {
        let Some(tx) = &self.cmd_tx else {
            return Err(TransportError::NotConnected);
        };
        tx.send(TdlibCommand::CheckPassword(password.to_string()))
            .map_err(|_| TransportError::NotConnected)
    }

    fn register_user(&mut self, first_name: &str, last_name: &str) -> Result<(), TransportError> {
        let Some(tx) = &self.cmd_tx else {
            return Err(TransportError::NotConnected);
        };
        tx.send(TdlibCommand::RegisterUser {
            first_name: first_name.trim().to_string(),
            last_name: last_name.trim().to_string(),
        }).map_err(|_| TransportError::NotConnected)
    }

    fn add_proxy(&mut self, server: &str, port: i32, secret: Option<&str>) -> Result<(), TransportError> {
        let Some(tx) = &self.cmd_tx else {
            return Err(TransportError::NotConnected);
        };
        tx.send(TdlibCommand::AddProxy {
            server: server.to_string(),
            port,
            secret: secret.map(|s| s.to_string()),
        }).map_err(|_| TransportError::NotConnected)
    }

    fn load_chats(&mut self, limit: i32) -> Result<(), TransportError> {
        let Some(tx) = &self.cmd_tx else {
            return Err(TransportError::NotConnected);
        };
        tx.send(TdlibCommand::LoadChats { limit })
            .map_err(|_| TransportError::NotConnected)
    }

    fn load_history(&mut self, dialog_id: i64, from_message_id: i64, limit: i32) -> Result<(), TransportError> {
        let Some(tx) = &self.cmd_tx else {
            return Err(TransportError::NotConnected);
        };
        tx.send(TdlibCommand::LoadChatHistory {
            chat_id: dialog_id,
            from_msg_id: from_message_id,
            limit,
        }).map_err(|_| TransportError::NotConnected)
    }

    fn mark_dialog_read(&mut self, dialog_id: i64) -> Result<(), TransportError> {
        let Some(tx) = &self.cmd_tx else {
            return Err(TransportError::NotConnected);
        };
        tx.send(TdlibCommand::MarkChatRead {
            chat_id: dialog_id,
        }).map_err(|_| TransportError::NotConnected)
    }

    fn get_chat(&mut self, chat_id: i64) -> Result<(), TransportError> {
        let Some(tx) = &self.cmd_tx else {
            return Err(TransportError::NotConnected);
        };
        tx.send(TdlibCommand::GetChat { chat_id }).map_err(|_| TransportError::NotConnected)
    }

    fn get_user(&mut self, user_id: i64) -> Result<(), TransportError> {
        let Some(tx) = &self.cmd_tx else {
            return Err(TransportError::NotConnected);
        };
        tx.send(TdlibCommand::GetUser { user_id }).map_err(|_| TransportError::NotConnected)
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn default_transport_initialization() {
        let transport = TdlibTransport::new_default();
        assert_eq!(transport.name(), "tdlib");
        assert_eq!(transport.params.api_id, DEFAULT_API_ID);
        assert_eq!(transport.params.api_hash, DEFAULT_API_HASH);
        assert!(!transport.params.use_secret_chats);
        assert_eq!(transport.auth_state(), AuthState::WaitingTdlibParameters);
    }

    #[test]
    fn transport_unconnected_commands_fail_gracefully() {
        let mut transport = TdlibTransport::new_default();
        assert!(matches!(transport.send_message(1, "test"), Err(TransportError::NotConnected)));
        assert!(matches!(transport.set_phone_number("+123456789"), Err(TransportError::NotConnected)));
        assert!(matches!(transport.check_auth_code("12345"), Err(TransportError::NotConnected)));
        assert!(matches!(transport.check_password("secret"), Err(TransportError::NotConnected)));
    }

    #[test]
    fn commands_preserve_full_chat_ids() {
        let mut transport = TdlibTransport::new_default();
        let (tx, mut rx) = tokio::sync::mpsc::unbounded_channel();
        transport.cmd_tx = Some(tx);
        let chat_id = -1_000_123_456_789;
        transport.send_message(chat_id, "test").unwrap();
        assert!(matches!(rx.try_recv().unwrap(), TdlibCommand::SendMessage { chat_id: id, .. } if id == chat_id));
        transport.load_history(chat_id, 123, 50).unwrap();
        assert!(matches!(rx.try_recv().unwrap(), TdlibCommand::LoadChatHistory { chat_id: id, .. } if id == chat_id));
        transport.mark_dialog_read(chat_id).unwrap();
        assert!(matches!(rx.try_recv().unwrap(), TdlibCommand::MarkChatRead { chat_id: id } if id == chat_id));
    }

    #[test]
    fn asynchronous_errors_are_consumed_once() {
        let transport = TdlibTransport::new_default();
        *transport.last_error.lock().unwrap() = Some("PHONE_CODE_INVALID".into());
        assert_eq!(transport.take_error().as_deref(), Some("PHONE_CODE_INVALID"));
        assert_eq!(transport.take_error(), None);
    }
}
