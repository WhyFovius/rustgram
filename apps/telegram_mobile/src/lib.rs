//! Rust/TDLib native core for the Android Compose client.
//!
//! Android owns the UI. Rust owns the Telegram session and exposes a very
//! small JNI boundary so Compose never has to know about TDLib internals.

#[cfg(target_os = "android")]
mod android_bridge {
    use std::sync::{Mutex, OnceLock};

    use jni::{objects::{JClass, JString}, sys::jstring, JNIEnv};
    use tg_core::{LocalStore, TdlibParameters, TdlibTransport, Transport};

    struct Session {
        client: TdlibTransport,
        store: LocalStore,
    }

    static TRANSPORT: OnceLock<Mutex<Option<Session>>> = OnceLock::new();

    fn transport() -> &'static Mutex<Option<Session>> {
        TRANSPORT.get_or_init(|| Mutex::new(None))
    }

    fn string(env: &mut JNIEnv<'_>, value: String) -> jstring {
        env.new_string(value).map(|s| s.into_raw()).unwrap_or(std::ptr::null_mut())
    }

    fn transport_lock() -> std::sync::MutexGuard<'static, Option<Session>> {
        transport().lock().unwrap_or_else(std::sync::PoisonError::into_inner)
    }

    #[no_mangle]
    pub extern "system" fn Java_com_rustgram_app_NativeBridge_start(
        mut env: JNIEnv<'_>, _: JClass<'_>, files: JString<'_>, database: JString<'_>,
    ) {
        let Ok(files) = env.get_string(&files) else { return };
        let Ok(database) = env.get_string(&database) else { return };
        let mut guard = transport_lock();
        if guard.is_some() {
            return;
        }
        let params = TdlibParameters {
            files_directory: files.to_string_lossy().into_owned(),
            database_directory: database.to_string_lossy().into_owned(),
            ..TdlibParameters::default()
        };
        let mut client = TdlibTransport::with_parameters(params);
        if client.start().is_ok() {
            *guard = Some(Session { client, store: LocalStore::default() });
        }
    }

    #[no_mangle]
    pub extern "system" fn Java_com_rustgram_app_NativeBridge_authState(
        mut env: JNIEnv<'_>, _: JClass<'_>,
    ) -> jstring {
        let value = transport_lock().as_mut()
            .map(|session| {
                // Consume the event queue even while the Compose chat UI is a preview.
                let _ = session.client.poll(&mut session.store);
                format!("{:?}", session.client.auth_state())
            })
            .unwrap_or_else(|| "Starting".into());
        string(&mut env, value)
    }

    #[no_mangle]
    pub extern "system" fn Java_com_rustgram_app_NativeBridge_takeError(
        mut env: JNIEnv<'_>, _: JClass<'_>,
    ) -> jstring {
        let error = transport_lock().as_ref()
            .and_then(|session| session.client.take_error())
            .unwrap_or_default();
        string(&mut env, error)
    }

    fn command(mut env: JNIEnv<'_>, value: JString<'_>, f: impl FnOnce(&mut TdlibTransport, &str)) {
        let Ok(value) = env.get_string(&value) else { return };
        if let Some(session) = transport_lock().as_mut() {
            f(&mut session.client, &value.to_string_lossy());
        }
    }

    #[no_mangle]
    pub extern "system" fn Java_com_rustgram_app_NativeBridge_setPhone(
        env: JNIEnv<'_>, _: JClass<'_>, value: JString<'_>,
    ) { command(env, value, |t, v| { let _ = t.set_phone_number(v); }); }

    #[no_mangle]
    pub extern "system" fn Java_com_rustgram_app_NativeBridge_setCode(
        env: JNIEnv<'_>, _: JClass<'_>, value: JString<'_>,
    ) { command(env, value, |t, v| { let _ = t.check_auth_code(v); }); }

    #[no_mangle]
    pub extern "system" fn Java_com_rustgram_app_NativeBridge_setPassword(
        env: JNIEnv<'_>, _: JClass<'_>, value: JString<'_>,
    ) { command(env, value, |t, v| { let _ = t.check_password(v); }); }

    #[no_mangle]
    pub extern "system" fn Java_com_rustgram_app_NativeBridge_getDialogs(
        mut env: JNIEnv<'_>, _: JClass<'_>,
    ) -> jstring {
        let json = transport_lock().as_mut()
            .map(|session| {
                let _ = session.client.poll(&mut session.store);
                serde_json::to_string(session.store.dialogs()).unwrap_or_else(|_| "[]".into())
            })
            .unwrap_or_else(|| "[]".into());
        string(&mut env, json)
    }

    #[no_mangle]
    pub extern "system" fn Java_com_rustgram_app_NativeBridge_getMessages(
        mut env: JNIEnv<'_>, _: JClass<'_>, dialog_id: jni::sys::jlong,
    ) -> jstring {
        let json = transport_lock().as_mut()
            .map(|session| {
                let _ = session.client.poll(&mut session.store);
                serde_json::to_string(session.store.messages(dialog_id)).unwrap_or_else(|_| "[]".into())
            })
            .unwrap_or_else(|| "[]".into());
        string(&mut env, json)
    }

    #[no_mangle]
    pub extern "system" fn Java_com_rustgram_app_NativeBridge_sendMessage(
        mut env: JNIEnv<'_>, _: JClass<'_>, dialog_id: jni::sys::jlong, text: JString<'_>,
    ) {
        let Ok(text) = env.get_string(&text) else { return };
        let text = text.to_string_lossy().into_owned();
        if let Some(session) = transport_lock().as_mut() {
            let _ = session.client.send_message(dialog_id, &text);
        }
    }

    #[no_mangle]
    pub extern "system" fn Java_com_rustgram_app_NativeBridge_markAsRead(
        _env: JNIEnv<'_>, _: JClass<'_>, dialog_id: jni::sys::jlong,
    ) {
        if let Some(session) = transport_lock().as_mut() {
            let _ = session.client.mark_dialog_read(dialog_id);
            session.store.mark_read(dialog_id);
        }
    }

    #[no_mangle]
    pub extern "system" fn Java_com_rustgram_app_NativeBridge_loadHistory(
        _env: JNIEnv<'_>, _: JClass<'_>, dialog_id: jni::sys::jlong,
    ) {
        if let Some(session) = transport_lock().as_mut() {
            let _ = session.client.load_history(dialog_id, 0, 50);
        }
    }

    #[no_mangle]
    pub extern "system" fn Java_com_rustgram_app_NativeBridge_loadChats(
        _env: JNIEnv<'_>, _: JClass<'_>, limit: jni::sys::jint,
    ) {
        if let Some(session) = transport_lock().as_mut() {
            let _ = session.client.load_chats(limit);
        }
    }
}
