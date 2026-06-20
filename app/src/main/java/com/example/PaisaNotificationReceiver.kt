package com.example

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.data.PaisaDataStore
import com.example.data.paisaPrefsDataStore
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.util.*

class PaisaNotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("PaisaNotification", "ALARM_TRIGGERED: Boot Completed. Rescheduling all alarms.")
            scheduleAllAlarms(context)
            return
        }

        Log.d("PaisaNotification", "ALARM_TRIGGERED")

        val alarmId = intent.getIntExtra("alarm_id", -1)
        val title = intent.getStringExtra("title") ?: "Paisa Tracker"
        val message = intent.getStringExtra("message") ?: "Keep your expenses updated!"

        val dataStore = PaisaDataStore(context)

        runBlocking {
            val globalEnabled = dataStore.isNotificationsEnabled.first()
            val dailyEnabled = dataStore.isDailyRemindersEnabled.first()
            val frequency = dataStore.reminderFrequency.first()

            if (!globalEnabled || !dailyEnabled) {
                Log.d("PaisaNotification", "Notification skipped: Global or Daily Reminders disabled")
                // Still reschedule the next run
                rescheduleAlarm(context, alarmId, title, message)
                return@runBlocking
            }

            // Filter by frequency
            // Alarm IDs:
            // 0: 08:00 AM (Freq 3, 5, 7)
            // 1: 11:00 AM (Freq 5, 7)
            // 2: 12:00 PM (Freq 7 only) - Smart logic
            // 3: 02:00 PM (Freq 3, 5, 7)
            // 4: 05:00 PM (Freq 5, 7)
            // 5: 08:00 PM (Freq 3, 5, 7)
            // 6: 10:00 PM (Freq 7 only)
            val isAllowed = when (frequency) {
                3 -> alarmId in listOf(0, 3, 5)
                5 -> alarmId in listOf(0, 1, 3, 4, 5)
                7 -> true
                else -> alarmId in listOf(0, 1, 3, 4, 5) // Fallback to 5
            }

            if (!isAllowed) {
                Log.d("PaisaNotification", "Notification skipped: alarmId $alarmId not allowed for frequency $frequency")
                rescheduleAlarm(context, alarmId, title, message)
                return@runBlocking
            }

            // Smart logic for 12:00 PM "No Activity Detected"
            if (alarmId == 2) {
                val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                val lastTxDate = context.paisaPrefsDataStore.data.first()[stringPreferencesKey("last_transaction_date")] ?: ""
                if (lastTxDate == todayDate) {
                    Log.d("PaisaNotification", "Notification skipped: Smart logic detected transaction already recorded today")
                    rescheduleAlarm(context, alarmId, title, message)
                    return@runBlocking
                }
            }

            // Deliver notification
            sendLocalNotification(context, alarmId, title, message)

            // Reschedule for next day (since setExact runs once)
            rescheduleAlarm(context, alarmId, title, message)
        }
    }

    private fun sendLocalNotification(context: Context, alarmId: Int, title: String, message: String) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = PaisaApplication.CHANNEL_REMINDERS

        val clickIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val clickPendingIntent = androidx.core.app.TaskStackBuilder.create(context).run {
            addNextIntentWithParentStack(clickIntent)
            getPendingIntent(
                alarmId,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(clickPendingIntent)
            .build()

        manager.notify(System.currentTimeMillis().toInt() + alarmId, notification)
        Log.d("PaisaNotification", "NOTIFICATION_SENT")
    }

    private fun rescheduleAlarm(context: Context, alarmId: Int, title: String, message: String) {
        // Find hour and minute from alarmId
        val (hour, minute) = when (alarmId) {
            0 -> 8 to 0
            1 -> 11 to 0
            2 -> 12 to 0
            3 -> 14 to 0
            4 -> 17 to 0
            5 -> 20 to 0
            6 -> 22 to 0
            else -> return
        }

        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        // Force schedule for tomorrow as we are rescheduling after firing
        calendar.add(Calendar.DAY_OF_YEAR, 1)

        val intent = Intent(context, PaisaNotificationReceiver::class.java).apply {
            putExtra("alarm_id", alarmId)
            putExtra("title", title)
            putExtra("message", message)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarmId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
            }
            Log.d("PaisaNotification", "NOTIFICATION_SCHEDULED: Rescheduled alarmId $alarmId for tomorrow")
        } catch (e: Exception) {
            Log.e("PaisaNotification", "Failed to reschedule alarmId $alarmId: ${e.message}")
        }
    }

    companion object {
        fun scheduleAllAlarms(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            val alarmSlots = listOf(
                Triple(0, 8 to 0, "Good Morning" to "Start your day by planning your expenses."),
                Triple(1, 11 to 0, "Expense Check" to "Have you recorded today's spending?"),
                Triple(2, 12 to 0, "No Activity Detected" to "You haven't recorded any transactions today."),
                Triple(3, 14 to 0, "Money Reminder" to "Keep your finances updated in Paisa Tracker."),
                Triple(4, 17 to 0, "Track Expenses" to "Don't forget to add your latest transactions."),
                Triple(5, 20 to 0, "Budget Reminder" to "Review today's expenses before the day ends."),
                Triple(6, 22 to 0, "Daily Summary" to "Check your income and expenses for today.")
            )

            for ((alarmId, time, info) in alarmSlots) {
                val (hour, minute) = time
                val (title, message) = info

                val calendar = Calendar.getInstance().apply {
                    timeInMillis = System.currentTimeMillis()
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, minute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                if (calendar.timeInMillis <= System.currentTimeMillis()) {
                    calendar.add(Calendar.DAY_OF_YEAR, 1)
                }

                val intent = Intent(context, PaisaNotificationReceiver::class.java).apply {
                    putExtra("alarm_id", alarmId)
                    putExtra("title", title)
                    putExtra("message", message)
                }
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    alarmId,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        if (alarmManager.canScheduleExactAlarms()) {
                            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
                        } else {
                            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
                        }
                    } else {
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
                    }
                    Log.d("PaisaNotification", "NOTIFICATION_SCHEDULED: Scheduled alarmId $alarmId at ${hour}:${minute}")
                } catch (e: Exception) {
                    Log.e("PaisaNotification", "Failed to schedule alarmId $alarmId: ${e.message}")
                }
            }
        }
    }
}
