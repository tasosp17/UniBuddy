package com.it2021084.unibuddy

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import java.util.Calendar

object CourseAlarmScheduler{
    fun scheduleAlarmsForCourses(context: Context, enrolledCourseIds: Set<String>){
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        //schedule new alarms
        var requestCodeBase = 1000 //unique ID counter
        for (id in enrolledCourseIds){
            val course = CourseCatalog.allCourses.find {it.id == id} ?: continue

            for (schedule in course.schedule){
                //calculate next occurrence
                val nextClassTime = getNextOccurrence(schedule.dayOfWeek, schedule.hour, schedule.minute)

                //subtract 10 minutes
                val triggerTime = nextClassTime - (10 * 60 * 1000)

                if (triggerTime < System.currentTimeMillis() - (30 * 60 * 1000)) {
                    continue
                }

                scheduleAlarm(context, alarmManager, course.name, triggerTime, requestCodeBase++)
            }
        }
    }

    private fun scheduleAlarm(context: Context, alarmManager: AlarmManager, courseName: String, timeInMillis: Long, requestCode: Int){
        val intent = Intent(context, LectureNotificationReceiver::class.java).apply{
            putExtra("courseName", courseName)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try{
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (!alarmManager.canScheduleExactAlarms()){
                    Log.e("AlarmScheduler", "Exact Alarm permission not granted!")
                    return
                }
            }
            //set the alarm
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent)
            }
            Log.d("AlarmScheduler", "Scheduled $courseName for ${java.util.Date(timeInMillis)}")
        } catch (e: SecurityException){
            Log.e("AlarmScheduler", "Error scheduling alarm: ${e.message}")
        }

    }

    //helper to calculate next class time
    private fun getNextOccurrence(targetDay: Int, targetHour: Int, targetMinute: Int): Long{
        val cal = Calendar.getInstance()
        val currentDay = cal.get(Calendar.DAY_OF_WEEK) // sun=1, mon= 2...

        //calculate difference
        var daysDiff = targetDay - currentDay

        //logic to handle scheduling
        if (daysDiff < 0){
            //day has passed this week, add 7 days to move to next
            daysDiff += 7
        } else if (daysDiff == 0){
            //same day, check time
            val currentHour = cal.get(Calendar.HOUR_OF_DAY)
            val currentMin = cal.get(Calendar.MINUTE)

            if (currentHour > targetHour || (currentHour == targetHour && currentMin >= targetMinute)){
                //lecture finished for today, move to next week
                daysDiff += 7
            }
        }

        cal.add(Calendar.DAY_OF_YEAR, daysDiff)
        cal.set(Calendar.HOUR_OF_DAY, targetHour)
        cal.set(Calendar.MINUTE, targetMinute)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        return cal.timeInMillis
    }

}