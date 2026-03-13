package com.it2021084.unibuddy

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import java.util.Random

class LectureNotificationReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null) return

        //check user's preferences
        val prefs = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val isEnabled = prefs.getBoolean("NOTIF_LECTURES", true) //default is true

        if (!isEnabled) { return } //stop if user has disabled lecture reminders

        val courseName = intent?.getStringExtra("courseName") ?: "Class"

        showNotification(context, courseName)
    }

    private fun showNotification(context: Context, courseName: String){
        val channelId = "course_reminders"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        //create channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            val channel = NotificationChannel(
                channelId, "Course Reminders", NotificationManager.IMPORTANCE_HIGH
            ).apply{
                description = "Reminders for upcoming lectures"
            }
            notificationManager.createNotificationChannel(channel)
        }

        //tap action: open MainActivity
        val tapIntent = Intent(context, MainActivity::class.java).apply{
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(context, 0, tapIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher_foreground)
            .setContentTitle("Upcoming Lecture")
            .setContentText("$courseName starts in 10 minutes!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(Random().nextInt(), notification)
    }
}