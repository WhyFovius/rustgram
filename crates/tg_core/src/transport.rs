//! Transport interface boundary for the future Telegram backend.
//!
//! TODO(tdlib-rs/ferogram): implement [`Transport`] against tdlib-rs
//! (prebuilt TDLib bindings) or ferogram (pure-Rust MTProto) and drive it
//! from a tokio worker, per task.md. Until then the UI keeps working off
//! [`LocalStore`](crate::LocalStore) alone.
//!
//! The shape is deliberately runtime-agnostic: the UI loop calls
//! [`Transport::poll`] each tick and receives [`CoreEvent`]s through an
//! [`EventSink`], so a later async implementation only has to enqueue events
//! from its worker thread and keep `send_message` non-blocking.
//!
//! Not wired into the app yet — this is the subscription point the frontend
//! will use once a real backend exists.

use std::fmt;

use crate::model::Message;

/// What a transport tells the UI about. Mirrors the mutations `LocalStore`
/// already performs, so the demo store and a real backend drive the same
/// refresh paths.
#[derive(Clone, Debug, PartialEq, Eq)]
pub enum CoreEvent {
    /// The full store was reloaded (startup sync or reconnect).
    StoreReloaded,
    /// The dialog list changed (new, removed or reordered dialogs).
    DialogsChanged,
    /// One dialog's preview/unread/state changed.
    DialogChanged { dialog_id: i64 },
    /// A single dialog was added or updated with full details.
    DialogUpserted(crate::model::Dialog),
    /// A batch of messages was received for a dialog.
    MessagesLoaded { dialog_id: i64, messages: Vec<Message> },
    /// A message was appended to a dialog.
    MessageAppended { dialog_id: i64, message: Message },
    /// Authentication state updated.
    AuthStateChanged { state: String },
    /// Connection state updated (Connecting, Ready, WaitingForNetwork, etc.)
    ConnectionStateChanged { state: String },
    /// A file download completed with local storage path.
    FileDownloaded { file_id: i32, path: String },
}

/// Receives events drained by [`Transport::poll`]. The UI implements this to
/// subscribe to backend updates.
pub trait EventSink: Send {
    fn on_event(&mut self, event: CoreEvent);
}

#[derive(Clone, Debug, PartialEq, Eq)]
pub enum TransportError {
    /// No live session; the caller should stay in the local-only mode.
    NotConnected,
    /// The operation exists but this transport does not support it.
    Unsupported(&'static str),
}

impl fmt::Display for TransportError {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        match self {
            TransportError::NotConnected => write!(f, "transport is not connected"),
            TransportError::Unsupported(what) => write!(f, "unsupported: {what}"),
        }
    }
}

impl std::error::Error for TransportError {}

/// A message transport (TDLib/MTProto in the future, nothing today).
///
/// Implementations must be cheap to poll and must never block on the network
/// inside `poll` or `send_message`; queue instead and let the worker thread
/// catch up.
pub trait Transport: Send {
    /// Stable identifier, e.g. `"tdlib"` or `"ferogram"`; for logs and settings.
    fn name(&self) -> &'static str;

    /// Connects / starts the worker. TODO(tdlib-rs): parameters, auth state
    /// and reconnect policy live here.
    fn start(&mut self) -> Result<(), TransportError>;

    /// Stops the worker; pending events may be dropped.
    fn stop(&mut self);

    /// Drains events produced since the last poll into `sink`.
    fn poll(&mut self, sink: &mut dyn EventSink) -> Result<(), TransportError>;

    /// Queues an outgoing message. Maps to TDLib `sendMessage` in real backend.
    fn send_message(&mut self, dialog_id: i64, body: &str) -> Result<(), TransportError>;

    /// Initiates phone-number authentication.
    fn set_phone_number(&mut self, phone: &str) -> Result<(), TransportError> {
        let _ = phone;
        Err(TransportError::Unsupported("authentication not supported by this transport"))
    }

    /// Submits the SMS/Telegram authentication code.
    fn check_auth_code(&mut self, code: &str) -> Result<(), TransportError> {
        let _ = code;
        Err(TransportError::Unsupported("authentication not supported by this transport"))
    }

    /// Submits the 2FA cloud password if requested.
    fn check_password(&mut self, password: &str) -> Result<(), TransportError> {
        let _ = password;
        Err(TransportError::Unsupported("authentication not supported by this transport"))
    }

    /// Completes registration for a new Telegram account.
    fn register_user(&mut self, first_name: &str, last_name: &str) -> Result<(), TransportError> {
        let _ = (first_name, last_name);
        Err(TransportError::Unsupported("registration not supported by this transport"))
    }

    /// Adds and enables an MTProto/Socks5 proxy for connection.
    fn add_proxy(&mut self, server: &str, port: i32, secret: Option<&str>) -> Result<(), TransportError> {
        let _ = (server, port, secret);
        Err(TransportError::Unsupported("proxy not supported by this transport"))
    }

    /// Requests TDLib to load more chats from the main chat list.
    fn load_chats(&mut self, limit: i32) -> Result<(), TransportError> {
        let _ = limit;
        Err(TransportError::Unsupported("load_chats not supported by this transport"))
    }

    /// Requests TDLib to load chat history for a dialog.
    fn load_history(&mut self, dialog_id: i64, from_message_id: i64, limit: i32) -> Result<(), TransportError> {
        let _ = (dialog_id, from_message_id, limit);
        Err(TransportError::Unsupported("load_history not supported by this transport"))
    }

    /// Marks dialog messages as read in TDLib.
    fn mark_dialog_read(&mut self, dialog_id: i64) -> Result<(), TransportError> {
        let _ = dialog_id;
        Err(TransportError::Unsupported("mark_dialog_read not supported by this transport"))
    }

    /// Fetches full chat details.
    fn get_chat(&mut self, chat_id: i64) -> Result<(), TransportError> {
        let _ = chat_id;
        Err(TransportError::Unsupported("get_chat not supported by this transport"))
    }

    /// Fetches user profile.
    fn get_user(&mut self, user_id: i64) -> Result<(), TransportError> {
        let _ = user_id;
        Err(TransportError::Unsupported("get_user not supported by this transport"))
    }
}

/// The demo transport: nothing is connected, nothing is emitted. Used as the
/// placeholder until tdlib-rs/ferogram lands, so the app can already hold and
/// poll a `Box<dyn Transport>`.
pub struct NullTransport;

impl Transport for NullTransport {
    fn name(&self) -> &'static str { "null" }

    fn start(&mut self) -> Result<(), TransportError> {
        Err(TransportError::Unsupported("null transport has no network"))
    }

    fn stop(&mut self) {}

    fn poll(&mut self, _sink: &mut dyn EventSink) -> Result<(), TransportError> {
        Ok(())
    }

    fn send_message(&mut self, _dialog_id: i64, _body: &str) -> Result<(), TransportError> {
        Err(TransportError::Unsupported("null transport has no network"))
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[derive(Default)]
    struct CountingSink {
        events: Vec<CoreEvent>,
    }

    impl EventSink for CountingSink {
        fn on_event(&mut self, event: CoreEvent) {
            self.events.push(event);
        }
    }

    #[test]
    fn null_transport_is_inert() {
        let mut transport = NullTransport;
        let mut sink = CountingSink::default();
        assert_eq!(transport.name(), "null");
        transport.poll(&mut sink).unwrap();
        assert!(sink.events.is_empty());
        transport.stop();
    }

    #[test]
    fn null_transport_refuses_network_ops() {
        let mut transport = NullTransport;
        assert_eq!(transport.start(), Err(TransportError::Unsupported("null transport has no network")));
        assert!(matches!(
            transport.send_message(1, "привет"),
            Err(TransportError::Unsupported(_))
        ));
    }

    #[test]
    fn sink_receives_events() {
        let mut sink = CountingSink::default();
        sink.on_event(CoreEvent::DialogsChanged);
        sink.on_event(CoreEvent::MessageAppended {
            dialog_id: 3,
            message: Message { body: "привет".into(), time: "Сейчас".into(), outgoing: false, read: false },
        });
        assert_eq!(sink.events.len(), 2);
        assert_eq!(sink.events[0], CoreEvent::DialogsChanged);
    }

    #[test]
    fn transport_error_displays() {
        assert_eq!(TransportError::NotConnected.to_string(), "transport is not connected");
        assert_eq!(TransportError::Unsupported("нет сети").to_string(), "unsupported: нет сети");
    }
}
