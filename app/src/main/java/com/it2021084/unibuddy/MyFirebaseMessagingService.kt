package com.it2021084.unibuddy

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import java.util.Random

class MyFirebaseMessagingService: FirebaseMessagingService() {

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val prefs = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val type = message.data["type"] ?: "chat"
        val title = message.data["title"] ?: "UniBuddy"
        val body = message.data["body"] ?: ""

        val senderName = message.data["senderName"] ?: title
        val senderAvatar = message.data["senderAvatar"] ?: ""

        when (type) {
            "chat" -> {
                val isChatEnabled = prefs.getBoolean("NOTIF_CHAT", true)
                if(!isChatEnabled) return

                val chatId = message.data["chatId"] ?: ""

                // Show the visual popup overlay layout
                showChatNotification(senderName, body, chatId)

                // Database logging manager call removed to prevent duplicate entries
            }
            "status" -> {
                val isChatEnabled = prefs.getBoolean("NOTIF_CHAT", true)
                if (!isChatEnabled) return

                val amIActive = prefs.getBoolean("AM_I_ACTIVE", false)
                if (!amIActive) return

                val userId = message.data["userId"] ?: ""
                showStatusNotification(senderName, body, userId)
            }
            "broadcast" -> {
                showBroadcastNotification(title, body)

                // Database logging manager call removed to prevent duplicate entries
            }
        }
    }

    private fun showChatNotification(title: String, body: String, chatId: String?) {
        val intent = Intent(this, ChatActivity::class.java).apply {
            if (chatId != null) {
                putExtra("chatId", chatId)
                putExtra("chatName", title)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
        }
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE)
        buildNotificationWindow(title, body, "chat_notifications", "Chat Messages", pendingIntent)
    }

    private fun showStatusNotification(title: String, body: String, userId: String?) {
        val intent = Intent(this, ProfileActivity::class.java).apply {
            if (userId != null) putExtra("userId", userId)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE)
        buildNotificationWindow(title, body, "status_notifications", "Online Status Updates", pendingIntent)
    }

    private fun showBroadcastNotification(title: String, body: String){
        val intent = Intent(this, BroadcastViewActivity::class.java).apply{
            putExtra("broadcast_body", body)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE)
        buildNotificationWindow(title, body, "secretary_broadcasts", "Official Announcements", pendingIntent)
    }

    private fun buildNotificationWindow(title: String, body: String, channelId: String, channelName: String, pendingIntent: PendingIntent){
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }
        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        notificationManager.notify(Random().nextInt(), builder.build())
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val currentUid = FirebaseAuth.getInstance().uid
        if (currentUid != null){
            FirebaseDatabase.getInstance("https://uni-buddy-it2021084-default-rtdb.europe-west1.firebasedatabase.app")
                .reference.child("users").child(currentUid).child("fcmToken").setValue(token)
        }
    }
}