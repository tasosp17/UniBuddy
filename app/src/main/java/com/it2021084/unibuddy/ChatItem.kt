package com.it2021084.unibuddy

data class ChatItem(
    var chatId: String = "",
    var isGroup: Boolean = false,
    var memberIds: List<String> = emptyList(),
    var username: String = "",
    var avatar: String? = null, //base64
    var lastMessage: String,
    var timestamp: Long,
    var isUnread: Boolean = false
)
