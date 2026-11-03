//! TDLib-backed transport and type mappings.
//!
//! Maps TDLib's generated types into the domain representations used by
//! `tg_core` and the UI:
//!
//! - [`tdlib_rs::types::Chat`] -> [`crate::model::Dialog`]
//! - [`tdlib_rs::types::Message`] -> [`crate::model::Message`]
//! - [`tdlib_rs::enums::Update`] -> [`crate::transport::CoreEvent`]
//!
//! Also handles authorization state tracking and login steps (phone number,
//! SMS code, 2FA cloud password) required for real Telegram sessions.

use crate::model::{Dialog, DialogCategory, Message};
use crate::transport::CoreEvent;

/// State of the TDLib authorization flow.
#[derive(Clone, Debug, PartialEq, Eq)]
pub enum AuthState {
    /// Initializing TDLib parameters.
    WaitingTdlibParameters,
    /// Waiting for the user's phone number.
    WaitingPhoneNumber,
    /// Waiting for the SMS/Telegram authentication code.
    WaitingCode {
        phone_number: String,
        is_registered: bool,
    },
    /// Waiting for the 2FA cloud password.
    WaitingPassword {
        password_hint: Option<String>,
        has_recovery_email: bool,
    },
    /// Waiting for the user's first/last name for a new account.
    WaitingRegistration,
    /// Session is fully authorized and ready.
    Ready,
    /// Logged out.
    LoggingOut,
    /// Session closed.
    Closed,
    /// Unknown or unhandled state.
    Unknown(String),
}

/// Parameters needed to initialize a TDLib instance.
#[derive(Clone, Debug)]
pub struct TdlibParameters {
    pub api_id: i32,
    pub api_hash: String,
    pub system_language_code: String,
    pub device_model: String,
    pub system_version: String,
    pub application_version: String,
    pub database_directory: String,
    pub files_directory: String,
    pub use_file_database: bool,
    pub use_chat_info_database: bool,
    pub use_message_database: bool,
    pub use_secret_chats: bool,
    pub use_test_dc: bool,
}

impl Default for TdlibParameters {
    fn default() -> Self {
        Self {
            api_id: 27048025,
            api_hash: "d4725bc976e1a1c784ffbb1a61748390".into(),
            system_language_code: "ru".into(),
            device_model: "Android Slint".into(),
            system_version: "Android".into(),
            application_version: env!("CARGO_PKG_VERSION").into(),
            database_directory: "tdlib-db".into(),
            files_directory: "tdlib-files".into(),
            use_file_database: true,
            use_chat_info_database: true,
            use_message_database: true,
            use_secret_chats: false,
            use_test_dc: false,
        }
    }
}

/// Maps a TDLib chat into a `tg_core::Dialog`.
pub fn map_chat(chat: &tdlib_rs::types::Chat) -> Dialog {
    let name = chat.title.clone();
    let initials = initials_of(&name);

    let (category, online) = match &chat.r#type {
        tdlib_rs::enums::ChatType::Private(_) => (DialogCategory::Personal, false),
        tdlib_rs::enums::ChatType::BasicGroup(_) => (DialogCategory::Group, false),
        tdlib_rs::enums::ChatType::Supergroup(sg) => {
            if sg.is_channel {
                (DialogCategory::Channel, false)
            } else {
                (DialogCategory::Group, false)
            }
        }
        tdlib_rs::enums::ChatType::Secret(_) => (DialogCategory::Personal, false),
    };

    let (preview, time) = if let Some(last_message) = &chat.last_message {
        (
            extract_message_preview(last_message),
            format_unix_time(last_message.date),
        )
    } else {
        (String::new(), String::new())
    };

    let photo_path = chat.photo.as_ref().and_then(|p| {
        let local = &p.small.local;
        if local.is_downloading_completed && !local.path.is_empty() {
            Some(local.path.clone())
        } else {
            None
        }
    });

    Dialog {
        id: chat.id,
        name,
        initials,
        preview,
        time,
        unread: chat.unread_count,
        tint: (chat.id.unsigned_abs() % 6) as i32,
        category,
        online,
        photo_path,
    }
}

/// Maps a TDLib message into a `tg_core::Message`.
pub fn map_message(msg: &tdlib_rs::types::Message, my_user_id: Option<i64>) -> Message {
    let outgoing = msg.is_outgoing
        || my_user_id
            .map(|uid| match &msg.sender_id {
                tdlib_rs::enums::MessageSender::User(u) => u.user_id == uid,
                _ => false,
            })
            .unwrap_or(false);

    let body = extract_message_text(msg);
    let time = format_unix_time(msg.date);
    // Interaction info contains view/reaction counts, not a read receipt.
    // Outgoing receipts require the chat's last_read_outbox_message_id.
    let read = !outgoing;

    Message {
        body,
        time,
        outgoing,
        read,
    }
}

/// Extracts readable preview text from a TDLib message.
pub fn extract_message_preview(msg: &tdlib_rs::types::Message) -> String {
    let text = extract_message_text(msg);
    if text.is_empty() {
        match &msg.content {
            tdlib_rs::enums::MessageContent::MessagePhoto(_) => "Фотография".into(),
            tdlib_rs::enums::MessageContent::MessageVideo(_) => "Видео".into(),
            tdlib_rs::enums::MessageContent::MessageAudio(_) => "Аудиозапись".into(),
            tdlib_rs::enums::MessageContent::MessageDocument(_) => "Файл".into(),
            tdlib_rs::enums::MessageContent::MessageVoiceNote(_) => "Голосовое сообщение".into(),
            tdlib_rs::enums::MessageContent::MessageSticker(_) => "Стикер".into(),
            tdlib_rs::enums::MessageContent::MessageAnimation(_) => "GIF".into(),
            tdlib_rs::enums::MessageContent::MessageLocation(_) => "Геолокация".into(),
            tdlib_rs::enums::MessageContent::MessageContact(_) => "Контакт".into(),
            tdlib_rs::enums::MessageContent::MessagePoll(_) => "Опрос".into(),
            _ => "Сообщение".into(),
        }
    } else {
        text.chars().take(72).collect()
    }
}

/// Extracts text content from a TDLib message.
pub fn extract_message_text(msg: &tdlib_rs::types::Message) -> String {
    match &msg.content {
        tdlib_rs::enums::MessageContent::MessageText(t) => t.text.text.clone(),
        tdlib_rs::enums::MessageContent::MessagePhoto(p) => p.caption.text.clone(),
        tdlib_rs::enums::MessageContent::MessageVideo(v) => v.caption.text.clone(),
        tdlib_rs::enums::MessageContent::MessageDocument(d) => d.caption.text.clone(),
        tdlib_rs::enums::MessageContent::MessageAudio(a) => a.caption.text.clone(),
        _ => String::new(),
    }
}

/// Translates a TDLib `Update` into zero or more `CoreEvent`s.
pub fn map_update(update: &tdlib_rs::enums::Update, my_user_id: Option<i64>) -> Option<CoreEvent> {
    match update {
        tdlib_rs::enums::Update::NewMessage(u) => Some(CoreEvent::MessageAppended {
            dialog_id: u.message.chat_id,
            message: map_message(&u.message, my_user_id),
        }),
        tdlib_rs::enums::Update::NewChat(_) => Some(CoreEvent::DialogsChanged),
        tdlib_rs::enums::Update::ChatLastMessage(u) => Some(CoreEvent::DialogChanged {
            dialog_id: u.chat_id,
        }),
        tdlib_rs::enums::Update::ChatReadInbox(u) => Some(CoreEvent::DialogChanged {
            dialog_id: u.chat_id,
        }),
        tdlib_rs::enums::Update::ChatReadOutbox(u) => Some(CoreEvent::DialogChanged {
            dialog_id: u.chat_id,
        }),
        tdlib_rs::enums::Update::ChatTitle(u) => Some(CoreEvent::DialogChanged {
            dialog_id: u.chat_id,
        }),
        tdlib_rs::enums::Update::ChatPosition(_) => Some(CoreEvent::DialogsChanged),
        tdlib_rs::enums::Update::AuthorizationState(a) => Some(CoreEvent::AuthStateChanged {
            state: format!("{:?}", map_auth_state(&a.authorization_state)),
        }),
        tdlib_rs::enums::Update::ConnectionState(c) => Some(CoreEvent::ConnectionStateChanged {
            state: format!("{:?}", c.state),
        }),
        tdlib_rs::enums::Update::MessageSendSucceeded(u) => Some(CoreEvent::DialogChanged {
            dialog_id: u.message.chat_id,
        }),
        tdlib_rs::enums::Update::DeleteMessages(u) => Some(CoreEvent::DialogChanged {
            dialog_id: u.chat_id,
        }),
        _ => None,
    }
}

/// Translates a TDLib `AuthorizationState` into [`AuthState`].
pub fn map_auth_state(state: &tdlib_rs::enums::AuthorizationState) -> AuthState {
    use tdlib_rs::enums::AuthorizationState::*;
    match state {
        WaitTdlibParameters => AuthState::WaitingTdlibParameters,
        WaitPhoneNumber => AuthState::WaitingPhoneNumber,
        WaitCode(c) => AuthState::WaitingCode {
            phone_number: c.code_info.phone_number.clone(),
            is_registered: false,
        },
        WaitPassword(p) => AuthState::WaitingPassword {
            password_hint: if p.password_hint.is_empty() {
                None
            } else {
                Some(p.password_hint.clone())
            },
            has_recovery_email: p.has_recovery_email_address,
        },
        WaitRegistration(_) => AuthState::WaitingRegistration,
        Ready => AuthState::Ready,
        LoggingOut => AuthState::LoggingOut,
        Closed => AuthState::Closed,
        other => AuthState::Unknown(format!("{other:?}")),
    }
}

fn initials_of(name: &str) -> String {
    let mut chars = name.split_whitespace().filter_map(|w| w.chars().next());
    match (chars.next(), chars.next()) {
        (Some(a), Some(b)) => format!("{a}{b}").to_uppercase(),
        (Some(a), None) => a.to_uppercase().to_string(),
        _ => "—".into(),
    }
}

fn format_unix_time(timestamp: i32) -> String {
    if timestamp <= 0 {
        return String::new();
    }
    // Simple HH:MM from seconds (UTC-agnostic representation for UI)
    let total_minutes = (timestamp / 60) % (24 * 60);
    let hours = (total_minutes / 60) % 24;
    let minutes = total_minutes % 60;
    format!("{hours:02}:{minutes:02}")
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn initials_extraction() {
        assert_eq!(initials_of("Аня Соколова"), "АС");
        assert_eq!(initials_of("Марк"), "М");
        assert_eq!(initials_of("Команда продукта и дизайна"), "КП");
        assert_eq!(initials_of(""), "—");
    }

    #[test]
    fn time_formatting() {
        assert_eq!(format_unix_time(0), "");
        assert_eq!(format_unix_time(-10), "");
        // 14:32:00 in seconds within day = 14 * 3600 + 32 * 60 = 52320
        assert_eq!(format_unix_time(52320), "14:32");
    }

    #[test]
    fn auth_state_variants_check() {
        let auth = AuthState::WaitingCode {
            phone_number: "+79990001122".into(),
            is_registered: true,
        };
        assert!(matches!(auth, AuthState::WaitingCode { .. }));
        assert_eq!(map_auth_state(&tdlib_rs::enums::AuthorizationState::Ready), AuthState::Ready);
        assert_eq!(map_auth_state(&tdlib_rs::enums::AuthorizationState::Closed), AuthState::Closed);
    }
}
