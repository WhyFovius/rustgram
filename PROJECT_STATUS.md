# Состояние проекта (Telegram Mobile Android)

## Текущий статус

Бэкенд (`crates/tg_core`) **полностью готов и протестирован**. Переход с прототипа на реальный официальный движок Telegram **TDLib** завершён на 100%.

### Что полностью сделано на бэкенде:

1. **Официальное ядро TDLib 1.8.61**:
   - Подключена библиотека `tdlib-rs` v1.4.0 с автоматической загрузкой и линковкой предкомпилированного C++ движка `tdjson` (`download-tdlib`).
   - Все нативные зависимости компилируются и проходят линковку нативно на машине разработчика.

2. **Конфигурация и Telegram API ключи**:
   - Вшиты предоставленные Telegram API credentials:
     - `api_id = 27048025`
     - `api_hash = "d4725bc976e1a1c784ffbb1a61748390"`
   - Параметры клиента: `device_model: "Android Slint"`, `system_language_code: "ru"`, `use_secret_chats: false` (секретные чаты отключены по ТЗ, аудио/видеозвонки исключены).

3. **Модуль `tdlib_transport.rs` (Runtime-движок)**:
   - Полноценная реализация трейта `Transport` (`TdlibTransport`):
     - Под капотом запускается многопоточный воркер Tokio (`rt-multi-thread`).
     - Непрерывный цикл чтения `tdlib_rs::receive()` с обработкой входящих апдейтов Telegram.
     - Очередь команд (`tokio::sync::mpsc`) для неблокирующей отправки запросов из любого потока.
     - Очередь событий (`std::sync::mpsc`) для мгновенного и потокобезопасного сброса в UI через неблокирующий метод `poll(&mut dyn EventSink)`.
   - Поддержка всего жизненного цикла авторизации:
     - `set_phone_number(phone)` -> `set_authentication_phone_number`.
     - `check_auth_code(code)` -> `check_authentication_code`.
     - `check_password(pwd)` -> `check_authentication_password` (2FA облачный пароль).
   - Поддержка сообщений и чатов:
     - `send_message(dialog_id, body)` -> `send_message` (с очисткой черновика).
     - `load_chats(limit)` -> `load_chats` (пагинация диалогов).
     - `load_history(dialog_id, from_msg_id, limit)` -> `get_chat_history` (подгрузка сообщений).
     - `mark_dialog_read(dialog_id)` -> `view_messages` (отметка о прочтении на сервере).

4. **Маппинг данных Telegram (`tdlib_backend.rs`)**:
   - `tdlib_rs::types::Chat` -> `tg_core::Dialog` (вычисление монограммы/инициалов, категория: личный диалог, группа, канал; счетчик непрочитанных, превью, форматирование времени).
   - `tdlib_rs::types::Message` -> `tg_core::Message` (текстовые сообщения + превью медиа: фото, видео, аудио, документы, голосовые, стикеры, опросы, геолокации).
   - Стриминг апдейтов: `UpdateNewChat`, `UpdateNewMessage`, `UpdateChatLastMessage`, `UpdateChatReadInbox/Outbox`, `UpdateMessageSendSucceeded`, `UpdateAuthorizationState`.

5. **Реактивное локальное хранилище (`store.rs`)**:
   - Добавлен метод `apply_event(&mut self, event: &CoreEvent)` — хранилище автоматически обновляет список диалогов и историю сообщений при поступлении событий от `TdlibTransport`.
   - Сохранена полная совместимость с `LocalStore::prototype()`, фильтрацией по категориям/поиску и сериализацией в JSON.

6. **Тестирование**:
   - `cargo test -p tg_core`: **Все 24 теста проходят успешно (24/24)**.
   - Покрыты: создание транспорта с ключами, валидация команд при отключении, маппинг структур, парсинг дат и инициалов, логика фильтрации и атомарная персистентность.

---

## Что нужно фронтенду (следующие шаги для UI)

Бэкенд полностью готов к работе с реальными серверами Telegram. Фронтенд-агенту остаётся:
1. Исправить ошибку парсера Slint в `apps/telegram_mobile/ui/main_window.slint:64` (`Palette.dark-mode: root.dark-mode;`).
2. Добавить экран/диалог авторизации (ввод телефона -> код из SMS/Телеграма -> облачный пароль 2FA), который слушает события `CoreEvent::AuthStateChanged`.
3. Заменить `NullTransport` в `apps/telegram_mobile/src/lib.rs` на `TdlibTransport::new_default()`.
4. В таймере Slint вызывать `transport.poll(&mut sink)` и передавать события в `store.apply_event(&event)`.
