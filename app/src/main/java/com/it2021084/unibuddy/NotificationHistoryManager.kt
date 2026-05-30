package com.it2021084.unibuddy

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

object NotificationHistoryManager {

    private const val TAG = "NOTIF_HISTORY"
    private const val DB_URL = "https://uni-buddy-it2021084-default-rtdb.europe-west1.firebasedatabase.app"

    fun saveNotificationToHistory(title: String, body: String, type: String, payloadId: String, senderName: String = "", senderAvatar: String = ""){
        val currentUid = FirebaseAuth.getInstance().uid
        if (currentUid == null) {
            Log.e(TAG, "Cannot save notification log: Current user UID is null!")
            return
        }

        val historyRef = FirebaseDatabase.getInstance(DB_URL)
            .getReference("users")
            .child(currentUid)
            .child("notificationHistory")

        val newLogRef = historyRef.push()
        val appNotif = AppNotification(
            id = newLogRef.key ?: "",
            title = title,
            body = body,
            type = type,
            timestamp = System.currentTimeMillis(),
            payloadId = payloadId,
            senderName = senderName,
            senderAvatar = senderAvatar
        )

        // Background Safe Direct Write: Bypasses sync listeners to ensure delivery
        newLogRef.setValue(appNotif).addOnSuccessListener {
            Log.d(TAG, "Notification background record written cleanly: ${appNotif.id}")
        }.addOnFailureListener { e ->
            Log.e(TAG, "Firebase background transaction failed: ${e.message}")
        }
    }
}