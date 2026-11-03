package com.rustgram.app

object NativeBridge {
    init { System.loadLibrary("tdjson"); System.loadLibrary("rustgram") }
    external fun start(files: String, database: String)
    external fun authState(): String
    external fun takeError(): String
    external fun setPhone(value: String)
    external fun setCode(value: String)
    external fun setPassword(value: String)
    external fun getDialogs(): String
    external fun getMessages(dialogId: Long): String
    external fun sendMessage(dialogId: Long, text: String)
    external fun markAsRead(dialogId: Long)
    external fun loadHistory(dialogId: Long)
    external fun loadChats(limit: Int)
    external fun getUser(userId: Long)
    external fun getChat(chatId: Long)
}
