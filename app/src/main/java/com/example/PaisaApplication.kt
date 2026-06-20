package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import android.util.Log
import com.google.firebase.database.FirebaseDatabase
import com.startapp.sdk.adsbase.StartAppSDK
import java.util.concurrent.TimeUnit
import androidx.core.app.NotificationCompat
import android.content.Intent
import android.app.PendingIntent
import androidx.core.app.TaskStackBuilder

class PaisaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // 1. Initialize AdManager and Start.io Ads SDK
        try {
            AdManager.initialize(this)
        } catch (e: Exception) {
            Log.e("PaisaAds", "AdManager init failed: ${e.message}")
        }

        // 2. Enable Firebase Realtime Database Offline Persistence
        try {
            FirebaseDatabase.getInstance().setPersistenceEnabled(true)
        } catch (e: Exception) {
            Log.e("PaisaApplication", "Failed to enable Firebase persistence: ${e.message}")
        }

        // 3. Create Notification Channels
        createNotificationChannels()

        // Log FCM Token
        try {
            com.google.firebase.messaging.FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val token = task.result
                    Log.d("PaisaNotification", "FCM_TOKEN_SUCCESS")
                    Log.d("PaisaNotification", "FCM_TOKEN: $token")
                } else {
                    Log.e("PaisaNotification", "FCM_TOKEN_FAILED: ${task.exception?.message}")
                }
            }
        } catch (e: Exception) {
            Log.e("PaisaNotification", "FCM_TOKEN_FAILED: ${e.message}")
        }

        // 4. Schedule Daily Custom Alarms via AlarmManager
        PaisaNotificationReceiver.scheduleAllAlarms(this)

        // 5. Schedule Background Periodic Work via WorkManager
        scheduleDailyReminder()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            // 1. Daily Reminder Channel
            val reminderChannel = NotificationChannel(
                CHANNEL_REMINDERS,
                "Daily Reminder",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Smart daily reminders to help you log and plan expenses."
            }
            manager.createNotificationChannel(reminderChannel)

            // 2. Budget Alerts Channel
            val budgetChannel = NotificationChannel(
                CHANNEL_BUDGETS,
                "Budget Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts and warnings when budget milestones are reached."
            }
            manager.createNotificationChannel(budgetChannel)

            // 3. Monthly Summary Channel
            val summaryChannel = NotificationChannel(
                CHANNEL_SUMMARY,
                "Monthly Summary",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Detailed local summaries delivered on the last day of each month."
            }
            manager.createNotificationChannel(summaryChannel)

            // 4. General Notifications Channel
            val generalChannel = NotificationChannel(
                CHANNEL_GENERAL,
                "General Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "General and system notifications from Paisa Tracker."
            }
            manager.createNotificationChannel(generalChannel)
        }
    }

    private fun scheduleDailyReminder() {
        try {
            Log.d("PaisaNotification", "WORKMANAGER_STARTED")
            // Interval mapped to 4 hours, firing 6 times a day (spanning 5 to 7 times dynamically)
            val workRequest = PeriodicWorkRequestBuilder<ReminderWorker>(4, TimeUnit.HOURS)
                .build()

            WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "paisa_daily_reminder",
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
            Log.d("PaisaNotification", "WORKMANAGER_SUCCESS")
        } catch (e: Exception) {
            Log.e("PaisaNotification", "Failed to schedule Daily Reminder WorkManager: ${e.message}")
        }
    }

    companion object {
        const val CHANNEL_REMINDERS = "paisa_reminders_channel"
        const val CHANNEL_BUDGETS = "paisa_budgets_channel"
        const val CHANNEL_SUMMARY = "paisa_summary_channel"
        const val CHANNEL_GENERAL = "paisa_general_channel"
    }
}

class ReminderWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        val tips = arrayOf(
            "✨ Paisa Tracker reminder: Fast food, tea or coffee today? Record your expenses in 5 seconds!",
            "📈 Financial Check: Compare your start and end dates in Analytics to spot saving trends.",
            "💰 Save first: 'Do not save what is left after spending, but spend what is left after saving.'",
            "🛡️ Secure & Offline: Paisa keeps your records private on this device.",
            "☕ Spent on food or beverage today? Log it under correct categories in Paisa to keep budgets green.",
            "📊 Daily Tip: Reviewing custom date ranges under analytics reveals hidden leakage trends.",
            "🚀 Target near! Check if your current monthly bills are pushing closer to your budget limit.",
            "💡 Smart spending: think twice before clicking buy. Delay non-essential purchases by 48 hours to prevent impulse buyer buying remorse."
        )

        try {
            val randomIndex = java.util.Random().nextInt(tips.size)
            val selectedTip = tips[randomIndex]
            
            val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            val clickIntent = Intent(applicationContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val clickPendingIntent = TaskStackBuilder.create(applicationContext).run {
                addNextIntentWithParentStack(clickIntent)
                getPendingIntent(
                    java.util.Random().nextInt(1000, 99999),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            }

            val notification = NotificationCompat.Builder(applicationContext, PaisaApplication.CHANNEL_REMINDERS)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Paisa Budget Assistant")
                .setContentText(selectedTip)
                .setStyle(NotificationCompat.BigTextStyle().bigText(selectedTip))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(clickPendingIntent)
                .build()

            manager.notify(java.util.Random().nextInt(1000, 99999), notification)
            Log.d("PaisaNotification", "NOTIFICATION_SENT")
        } catch (e: Exception) {
            Log.e("ReminderWorker", "Failed to show dynamic notification: ${e.message}")
        }
        return Result.success()
    }
}
