package com.it2021084.unibuddy

data class AppNotification(
    val id: String = "",
    val title: String = "",
    val body: String = "",
    val type: String = "", // "chat", "status", "lecture", "broadcast"
    val timestamp: Long = 0L,
    val payloadId: String = "",
    val senderName: String = "",
    val senderAvatar: String = "" // Holds a Base64 profile string or Cloudinary asset route
)