//! Conversation data and store.
//!
//! The network client connects via official TDLib / MTProto.


pub mod model;
pub mod persistence;
pub mod store;
pub mod tdlib_backend;
pub mod tdlib_transport;
pub mod transport;

pub use model::{Dialog, DialogCategory, Message};
pub use store::LocalStore;
pub use tdlib_backend::{AuthState, TdlibParameters};
pub use tdlib_transport::{TdlibTransport, DEFAULT_API_ID, DEFAULT_API_HASH};
pub use transport::{CoreEvent, EventSink, Transport, TransportError};
