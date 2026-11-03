//! Core conversation types shared by the store and the future transport.

use serde::{Deserialize, Serialize};

#[derive(Clone, Debug, PartialEq, Eq, Serialize, Deserialize)]
pub struct Dialog {
    pub id: i64,
    pub name: String,
    pub initials: String,
    pub preview: String,
    pub time: String,
    pub unread: i32,
    pub tint: i32,
    pub category: DialogCategory,
    pub online: bool,
    pub photo_path: Option<String>,
}

#[derive(Clone, Copy, Debug, PartialEq, Eq, Serialize, Deserialize)]
pub enum DialogCategory {
    Personal,
    Group,
}

#[derive(Clone, Debug, PartialEq, Eq, Serialize, Deserialize)]
pub struct Message {
    pub body: String,
    pub time: String,
    pub outgoing: bool,
    pub read: bool,
}
