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

        //check if the user has disabled chat notifications
        val prefs = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)

        //check type of message
        val type = message.data["type"] ?: "chat"

        if (type == "chat"){
            //chat logic
            val isChatEnabled = prefs.getBoolean("NOTIF_CHAT", true)
            if (!isChatEnabled) return

            val title = message.data["title"] ?: "UniBuddy"
            val body = message.data["body"] ?: "New Message"
            val chatId = message.data["chatId"]
            showChatNotification(title, body, chatId)
        } else if (type == "status"){
            //status logic
            val isChatEnabled = prefs.getBoolean("NOTIF_CHAT", true)
            if (!isChatEnabled) return

            // If I am NOT at school (isActive == false), ignore this notification.
            val amIActive = prefs.getBoolean("AM_I_ACTIVE", false)
            if (!amIActive) return

            val title = message.data["title"] ?: "UniBuddy"
            val body = message.data["body"] ?: "is now online!"
            val userId = message.data["userId"]
            showStatusNotification(title, body, userId)
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

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = "chat_notifications"
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        //create channel for android O+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "Chat Messages", NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        notificationManager.notify(Random().nextInt(), notificationBuilder.build())
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)

        //if the user is currently logged in, update the token in the database immediately
        val currentUid = FirebaseAuth.getInstance().uid

        if (currentUid != null){
            val dbRef = FirebaseDatabase.getInstance().reference
            dbRef.child("users").child(currentUid).child("fcmToken").setValue(token)
        }
    }

    private fun showStatusNotification(title: String, body: String, userId: String?){
        //tap action: open user's profile
        val intent = Intent(this, ProfileActivity::class.java).apply{
            if (userId != null){
                putExtra("userId", userId)
            }
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = "status_notifications"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            val channel = NotificationChannel(
                channelId, "Online Status Updates", NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        notificationManager.notify(Random().nextInt(), notificationBuilder.build())
    }
}