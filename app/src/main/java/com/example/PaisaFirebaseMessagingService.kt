package com.example
 
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.TaskStackBuilder
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
 
class PaisaFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("PaisaNotification", "FCM_TOKEN_SUCCESS")
        Log.d("PaisaNotification", "FCM_TOKEN: $token")
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d("PaisaNotification", "FCM_RECEIVED")

        if (MainActivity.isResumed) {
            Log.d("PaisaNotification", "FCM_FOREGROUND")
        } else if (MainActivity.isCreated) {
            Log.d("PaisaNotification", "FCM_BACKGROUND")
        } else {
            Log.d("PaisaNotification", "FCM_APP_CLOSED")
        }

        // Retrieve title and body
        val title = remoteMessage.notification?.title ?: remoteMessage.data["title"] ?: "Paisa Tracker"
        val body = remoteMessage.notification?.body ?: remoteMessage.data["body"] ?: "Check your latest updates!"

        sendNotification(title, body)
    }

    private fun sendNotification(title: String, body: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = PaisaApplication.CHANNEL_GENERAL

        val clickIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val clickPendingIntent = TaskStackBuilder.create(this).run {
            addNextIntentWithParentStack(clickIntent)
            getPendingIntent(
                0,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(clickPendingIntent)

        Log.d("PaisaNotification", "NOTIFICATION_SENT")
        manager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }
}
