//! The in-memory store for chats and messages, updated reactively by TDLib events.

use std::collections::HashMap;

use crate::model::{Dialog, DialogCategory, Message};
use crate::persistence;

#[derive(Default, Debug, Clone)]
pub struct LocalStore {
    dialogs: Vec<Dialog>,
    messages: HashMap<i64, Vec<Message>>,
}

impl crate::transport::EventSink for LocalStore {
    fn on_event(&mut self, event: crate::transport::CoreEvent) {
        self.apply_event(&event);
    }
}

impl LocalStore {
    pub fn new() -> Self {
        Self::default()
    }

    #[cfg(test)]
    pub fn test_seed() -> Self {
        let dialogs = vec![
            Dialog { id: 1, name: "Аня Соколова".into(), initials: "АС".into(), preview: "Буду через десять минут".into(), time: "14:32".into(), unread: 2, tint: 0, category: DialogCategory::Personal, online: true, photo_path: None },
            Dialog { id: 2, name: "Команда продукта".into(), initials: "КП".into(), preview: "Илья: макет уже в Figma".into(), time: "13:58".into(), unread: 5, tint: 1, category: DialogCategory::Group, online: false, photo_path: None },
            Dialog { id: 3, name: "Марк".into(), initials: "М".into(), preview: "Спасибо, получил".into(), time: "12:41".into(), unread: 0, tint: 2, category: DialogCategory::Personal, online: false, photo_path: None },
            Dialog { id: 4, name: "Поездка в Казань".into(), initials: "КЗ".into(), preview: "Лена: отправила адрес".into(), time: "11:16".into(), unread: 0, tint: 3, category: DialogCategory::Group, online: false, photo_path: None },
            Dialog { id: 5, name: "Даша".into(), initials: "Д".into(), preview: "Давай созвонимся вечером".into(), time: "Вчера".into(), unread: 0, tint: 4, category: DialogCategory::Personal, online: true, photo_path: None },
            Dialog { id: 6, name: "Заметки для себя".into(), initials: "З".into(), preview: "Список покупок".into(), time: "Вт".into(), unread: 0, tint: 5, category: DialogCategory::Personal, online: false, photo_path: None },
        ];
        let mut messages = HashMap::new();
        messages.insert(1, vec![
            Message { body: "Привет! Ты уже посмотрела маршрут?".into(), time: "14:24".into(), outgoing: false, read: false },
            Message { body: "Да, всё рядом. Встретимся у входа в парк".into(), time: "14:27".into(), outgoing: true, read: true },
            Message { body: "Буду через десять минут".into(), time: "14:32".into(), outgoing: false, read: false },
        ]);
        messages.insert(2, vec![
            Message { body: "Собрала заметки после встречи".into(), time: "13:46".into(), outgoing: false, read: false },
            Message { body: "Спасибо. Добавлю правки до конца дня".into(), time: "13:51".into(), outgoing: true, read: true },
            Message { body: "Макет уже в Figma".into(), time: "13:58".into(), outgoing: false, read: false },
        ]);
        messages.insert(3, vec![
            Message { body: "Отправил файлы, проверь пожалуйста".into(), time: "12:35".into(), outgoing: true, read: true },
            Message { body: "Спасибо, получил".into(), time: "12:41".into(), outgoing: false, read: false },
        ]);
        messages.insert(4, vec![
            Message { body: "Билеты на пятницу уже куплены".into(), time: "10:52".into(), outgoing: true, read: true },
            Message { body: "Отправила адрес гостиницы".into(), time: "11:16".into(), outgoing: false, read: false },
        ]);
        messages.insert(5, vec![
            Message { body: "Свободна после семи?".into(), time: "Вчера".into(), outgoing: true, read: true },
            Message { body: "Давай созвонимся вечером".into(), time: "Вчера".into(), outgoing: false, read: false },
        ]);
        messages.insert(6, vec![
            Message { body: "Список покупок".into(), time: "Вт".into(), outgoing: true, read: true },
        ]);
        Self { dialogs, messages }
    }

    pub fn dialogs(&self) -> &[Dialog] { &self.dialogs }

    pub fn filtered_dialogs(&self, query: &str, category: i32) -> Vec<Dialog> {
        let query = query.trim().to_lowercase();
        self.dialogs.iter().filter(|dialog| {
            let category_matches = category == 0
                || (category == 1 && dialog.category == DialogCategory::Personal)
                || (category == 2 && dialog.category == DialogCategory::Group);
            let query_matches = query.is_empty()
                || dialog.name.to_lowercase().contains(&query)
                || dialog.preview.to_lowercase().contains(&query);
            category_matches && query_matches
        }).cloned().collect()
    }

    pub fn messages(&self, dialog_id: i64) -> &[Message] {
        self.messages.get(&dialog_id).map(Vec::as_slice).unwrap_or(&[])
    }

    /// Replaces all dialogs with a new set (e.g. from backend sync).
    pub fn set_dialogs(&mut self, dialogs: Vec<Dialog>) {
        self.dialogs = dialogs;
    }

    /// Upserts a dialog into the store.
    pub fn upsert_dialog(&mut self, dialog: Dialog) {
        if let Some(pos) = self.dialogs.iter().position(|d| d.id == dialog.id) {
            self.dialogs[pos] = dialog;
        } else {
            self.dialogs.push(dialog);
        }
    }

    /// Appends a message to a dialog's history.
    pub fn append_message(&mut self, dialog_id: i64, message: Message) {
        self.messages.entry(dialog_id).or_default().push(message);
    }

    /// Sets the entire message list for a dialog.
    pub fn set_messages(&mut self, dialog_id: i64, messages: Vec<Message>) {
        self.messages.insert(dialog_id, messages);
    }

    pub fn apply_event(&mut self, event: &crate::transport::CoreEvent) {
        use crate::transport::CoreEvent::*;
        match event {
            DialogUpserted(dialog) => {
                self.upsert_dialog(dialog.clone());
            }
            MessagesLoaded { dialog_id, messages } => {
                self.set_messages(*dialog_id, messages.clone());
            }
            MessageAppended { dialog_id, message } => {
                if let Some(dialog) = self.dialogs.iter_mut().find(|d| d.id == *dialog_id) {
                    dialog.preview = message.body.chars().take(72).collect();
                    dialog.time = message.time.clone();
                    if !message.outgoing {
                        dialog.unread = dialog.unread.saturating_add(1);
                    }
                }
                self.append_message(*dialog_id, message.clone());
            }
            _ => {}
        }
    }

    pub fn send(&mut self, dialog_id: i64, body: String) -> bool {
        let body = body.trim();
        if body.is_empty() || !self.dialogs.iter().any(|dialog| dialog.id == dialog_id) {
            return false;
        }
        let preview = body.chars().take(72).collect::<String>();
        if let Some(dialog) = self.dialogs.iter_mut().find(|dialog| dialog.id == dialog_id) {
            dialog.preview = preview;
            dialog.time = "Сейчас".into();
        }
        self.messages.entry(dialog_id).or_default().push(Message {
            body: body.to_owned(),
            time: "Сейчас".into(),
            outgoing: true,
            read: false,
        });
        true
    }

    /// Marks a dialog read: clears its unread counter and flags incoming messages.
    /// Returns false for an unknown dialog id, true if anything changed.
    pub fn mark_read(&mut self, dialog_id: i64) -> bool {
        let Some(dialog) = self.dialogs.iter_mut().find(|dialog| dialog.id == dialog_id) else {
            return false;
        };
        let unread = dialog.unread;
        dialog.unread = 0;
        let mut changed = unread > 0;
        if let Some(messages) = self.messages.get_mut(&dialog_id) {
            for message in messages.iter_mut() {
                if !message.outgoing && !message.read {
                    message.read = true;
                    changed = true;
                }
            }
        }
        changed
    }

    /// Persists the whole store as JSON; see [`persistence`] for the format.
    pub fn save_to(&self, path: &std::path::Path) -> std::io::Result<()> {
        persistence::save(self, path)
    }

    /// Restores a store saved by [`LocalStore::save_to`]. Missing or
    /// unreadable files surface as errors.
    pub fn load_from(path: &std::path::Path) -> std::io::Result<Self> {
        persistence::load(path)
    }

    /// Assembles a store from raw parts; persistence only.
    pub(crate) fn from_parts(dialogs: Vec<Dialog>, messages: HashMap<i64, Vec<Message>>) -> Self {
        Self { dialogs, messages }
    }

    /// Read access to the full message map; persistence only.
    pub(crate) fn messages_map(&self) -> &HashMap<i64, Vec<Message>> {
        &self.messages
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn filter_by_category() {
        let store = LocalStore::test_seed();
        assert_eq!(store.filtered_dialogs("", 0).len(), 6);
        assert_eq!(store.filtered_dialogs("", 1).len(), 4);
        assert_eq!(store.filtered_dialogs("", 2).len(), 2);
        assert!(store.filtered_dialogs("", 2).iter().all(|d| d.category == DialogCategory::Group));
        assert!(store.filtered_dialogs("", 1).iter().all(|d| d.category == DialogCategory::Personal));
    }

    #[test]
    fn filter_by_query_matches_name_or_preview() {
        let store = LocalStore::test_seed();
        assert_eq!(store.filtered_dialogs("Марк", 0).len(), 1);
        assert_eq!(store.filtered_dialogs("Figma", 0).len(), 1);
        assert_eq!(store.filtered_dialogs("  аня  ", 0).len(), 1);
        assert_eq!(store.filtered_dialogs("казань", 0).len(), 1);
        assert!(store.filtered_dialogs("нет такого текста", 0).is_empty());
    }

    #[test]
    fn filter_combines_query_and_category() {
        let store = LocalStore::test_seed();
        assert_eq!(store.filtered_dialogs("поездка", 1).len(), 0);
        assert_eq!(store.filtered_dialogs("поездка", 2).len(), 1);
        assert_eq!(store.filtered_dialogs("поездка", 0).len(), 1);
    }

    #[test]
    fn send_appends_and_updates_dialog() {
        let mut store = LocalStore::test_seed();
        let before = store.messages(1).len();
        assert!(store.send(1, "Проверю и отвечу".into()));
        let messages = store.messages(1);
        assert_eq!(messages.len(), before + 1);
        assert_eq!(messages.last().unwrap().body, "Проверю и отвечу");
        assert!(messages.last().unwrap().outgoing);
        assert!(!messages.last().unwrap().read);
        let dialog = &store.dialogs()[0];
        assert_eq!(dialog.preview, "Проверю и отвечу");
        assert_eq!(dialog.time, "Сейчас");
    }

    #[test]
    fn send_rejects_blank_body_and_unknown_dialog() {
        let mut store = LocalStore::test_seed();
        assert!(!store.send(1, "".into()));
        assert!(!store.send(1, "   ".into()));
        assert!(!store.send(99, "привет".into()));
        assert_eq!(store.messages(1).len(), 3);
    }

    #[test]
    fn send_trims_body_and_caps_preview() {
        let mut store = LocalStore::test_seed();
        let long = "х".repeat(100);
        assert!(store.send(3, format!("  {}  ", long)));
        let message = store.messages(3).last().unwrap();
        assert_eq!(message.body.chars().count(), 100);
        assert_eq!(store.dialogs()[2].preview.chars().count(), 72);
    }

    #[test]
    fn mark_read_clears_unread_and_flags_messages() {
        let mut store = LocalStore::test_seed();
        assert!(store.mark_read(1));
        let dialog = &store.dialogs()[0];
        assert_eq!(dialog.unread, 0);
        assert!(store.messages(1).iter().all(|m| m.read));
        // Already read: nothing changes, reports false.
        assert!(!store.mark_read(1));
        assert!(!store.mark_read(99));
    }

    #[test]
    fn messages_of_unknown_dialog_is_empty() {
        let store = LocalStore::test_seed();
        assert!(store.messages(42).is_empty());
    }

    #[test]
    fn large_dialog_ids_do_not_collide() {
        let mut store = LocalStore::default();
        let mut first = LocalStore::test_seed().dialogs()[0].clone();
        first.id = -1_000_123_456_789;
        let mut second = first.clone();
        second.id += 1_i64 << 32;
        store.upsert_dialog(first.clone());
        store.upsert_dialog(second.clone());
        assert!(store.send(first.id, "first".into()));
        assert!(store.send(second.id, "second".into()));
        assert_eq!(store.dialogs().len(), 2);
        assert_eq!(store.messages(first.id)[0].body, "first");
        assert_eq!(store.messages(second.id)[0].body, "second");
    }

    #[test]
    fn marking_inbox_read_does_not_mark_outgoing_messages_read() {
        let mut store = LocalStore::test_seed();
        store.send(1, "not read by recipient".into());
        assert!(store.mark_read(1));
        assert!(!store.messages(1).last().unwrap().read);
        assert!(!store.mark_read(1));
        store.append_message(1, Message {
            body: "incoming".into(), time: String::new(), outgoing: false, read: false,
        });
        assert!(store.mark_read(1));
    }

    #[test]
    fn unread_count_saturates() {
        let mut store = LocalStore::test_seed();
        store.dialogs[0].unread = i32::MAX;
        store.apply_event(&crate::transport::CoreEvent::MessageAppended {
            dialog_id: 1,
            message: Message { body: "incoming".into(), time: String::new(), outgoing: false, read: false },
        });
        assert_eq!(store.dialogs()[0].unread, i32::MAX);
    }
}
