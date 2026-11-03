//! JSON persistence for [`LocalStore`], keeping the demonstrative data
//! character: the file only round-trips what the in-memory store holds.
//!
//! Format: a versioned envelope
//! `{"version":1,"dialogs":[...],"messages":{"1":[...]}}`. Messages are
//! written as a sorted map so the file is stable across runs. Writes go to
//! a sibling temp file first and are then renamed over the target.

use std::collections::BTreeMap;
use std::io;
use std::path::Path;

use serde::{Deserialize, Serialize};

use crate::model::{Dialog, Message};
use crate::store::LocalStore;

const FORMAT_VERSION: i32 = 1;

#[derive(Serialize, Deserialize)]
struct StoreFile {
    version: i32,
    dialogs: Vec<Dialog>,
    messages: BTreeMap<i64, Vec<Message>>,
}

fn to_file(store: &LocalStore) -> StoreFile {
    StoreFile {
        version: FORMAT_VERSION,
        dialogs: store.dialogs().to_vec(),
        messages: store
            .messages_map()
            .iter()
            .map(|(id, msgs)| (*id, msgs.clone()))
            .collect(),
    }
}

fn from_file(file: StoreFile) -> LocalStore {
    LocalStore::from_parts(file.dialogs, file.messages.into_iter().collect())
}

pub(crate) fn save(store: &LocalStore, path: &Path) -> io::Result<()> {
    let json = serde_json::to_string_pretty(&to_file(store))
        .map_err(|e| io::Error::new(io::ErrorKind::InvalidData, e))?;
    let tmp = path.with_extension("tmp");
    std::fs::write(&tmp, json)?;
    // std::fs::rename replaces an existing destination on Windows too.
    std::fs::rename(&tmp, path)
}

pub(crate) fn load(path: &Path) -> io::Result<LocalStore> {
    let json = std::fs::read_to_string(path)?;
    let file: StoreFile = serde_json::from_str(&json)
        .map_err(|e| io::Error::new(io::ErrorKind::InvalidData, e))?;
    if file.version != FORMAT_VERSION {
        return Err(io::Error::new(
            io::ErrorKind::InvalidData,
            format!("unsupported store format version {}", file.version),
        ));
    }
    Ok(from_file(file))
}

#[cfg(test)]
mod tests {
    use super::*;

    fn temp_path(tag: &str) -> std::path::PathBuf {
        let dir = std::env::temp_dir()
            .join(format!("tg_core_persist_{}_{}", std::process::id(), tag));
        std::fs::create_dir_all(&dir).unwrap();
        dir.join("store.json")
    }

    #[test]
    fn roundtrip_preserves_state() {
        let path = temp_path("roundtrip");
        let mut store = LocalStore::test_seed();
        store.send(1, "проверка сохранения".into());
        store.mark_read(2);
        store.save_to(&path).unwrap();
        let loaded = LocalStore::load_from(&path).unwrap();
        assert_eq!(loaded.dialogs(), store.dialogs());
        assert_eq!(loaded.messages(1), store.messages(1));
        assert_eq!(loaded.messages(2), store.messages(2));
        let _ = std::fs::remove_file(&path);
    }

    #[test]
    fn empty_store_roundtrips() {
        let path = temp_path("empty");
        let store = LocalStore::default();
        store.save_to(&path).unwrap();
        let loaded = LocalStore::load_from(&path).unwrap();
        assert!(loaded.dialogs().is_empty());
        assert!(loaded.messages(0).is_empty());
        let _ = std::fs::remove_file(&path);
    }

    #[test]
    fn large_dialog_ids_roundtrip() {
        let path = temp_path("large_ids");
        let mut dialog = LocalStore::test_seed().dialogs()[0].clone();
        dialog.id = -1_000_123_456_789;
        let mut store = LocalStore::default();
        store.upsert_dialog(dialog.clone());
        store.send(dialog.id, "large id".into());
        store.save_to(&path).unwrap();
        let loaded = LocalStore::load_from(&path).unwrap();
        assert_eq!(loaded.dialogs(), store.dialogs());
        assert_eq!(loaded.messages(dialog.id), store.messages(dialog.id));
        let _ = std::fs::remove_file(&path);
    }

    #[test]
    fn save_overwrites_previous_file() {
        let path = temp_path("overwrite");
        LocalStore::test_seed().save_to(&path).unwrap();
        let mut second = LocalStore::default();
        assert!(!second.send(0, "нужен диалог".into()));
        second.save_to(&path).unwrap();
        let loaded = LocalStore::load_from(&path).unwrap();
        assert!(loaded.dialogs().is_empty());
        let _ = std::fs::remove_file(&path);
    }

    #[test]
    fn load_reports_missing_file() {
        let path = temp_path("missing").with_file_name("absent.json");
        let err = LocalStore::load_from(&path).unwrap_err();
        assert_eq!(err.kind(), io::ErrorKind::NotFound);
    }

    #[test]
    fn load_rejects_corrupt_file() {
        let path = temp_path("corrupt");
        std::fs::write(&path, "{ not json").unwrap();
        let err = LocalStore::load_from(&path).unwrap_err();
        assert_eq!(err.kind(), io::ErrorKind::InvalidData);
        let _ = std::fs::remove_file(&path);
    }

    #[test]
    fn load_rejects_unknown_version() {
        let path = temp_path("version");
        std::fs::write(&path, r#"{"version":99,"dialogs":[],"messages":{}}"#).unwrap();
        let err = LocalStore::load_from(&path).unwrap_err();
        assert_eq!(err.kind(), io::ErrorKind::InvalidData);
        let _ = std::fs::remove_file(&path);
    }

    #[test]
    fn file_is_versioned_and_sorted() {
        let path = temp_path("format");
        LocalStore::test_seed().save_to(&path).unwrap();
        let json = std::fs::read_to_string(&path).unwrap();
        assert!(json.contains("\"version\": 1"));
        assert!(json.find("\"1\":").unwrap() < json.find("\"2\":").unwrap());
        let _ = std::fs::remove_file(&path);
    }
}
