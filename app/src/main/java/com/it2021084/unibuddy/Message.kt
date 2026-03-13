package com.it2021084.unibuddy

data class Message(
    var senderId: String = "",
    var senderName:String = "",
    var senderAvatar:String? = null, //base64 string
    var message: String = "",
    var timestamp: Long = 0L
)
