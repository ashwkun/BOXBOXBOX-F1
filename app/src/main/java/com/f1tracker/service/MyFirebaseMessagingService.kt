package com.f1tracker.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.f1tracker.MainActivity
import com.f1tracker.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        // Handle DATA-ONLY payload (notification + data bypass onMessageReceived when app in background)
        if (remoteMessage.data.isNotEmpty()) {
            android.util.Log.d("FCM", "Data payload received: ${remoteMessage.data}")
            
            // Read title and body from data payload (for data-only messages)
            val title = remoteMessage.data["title"]
            val body = remoteMessage.data["body"]
            val url = remoteMessage.data["url"]
            val imageUrl = remoteMessage.data["image_url"]
            val channelId = remoteMessage.data["channel_id"]
            val targetTab = remoteMessage.data["target_tab"]
            
            if (!title.isNullOrEmpty() && !body.isNullOrEmpty()) {
                sendNotification(title, body, channelId, imageUrl, url, targetTab)
            } else {
                android.util.Log.w("FCM", "Missing title or body in data payload")
            }
        }
    }

    override fun onNewToken(token: String) {
        // Send token to server if needed
        android.util.Log.d("FCM", "Refreshed token: $token")
    }

    private fun sendNotification(title: String?, messageBody: String?, channelId: String?, imageUrl: String?, url: String?, targetTab: String?) {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        
        if (!url.isNullOrEmpty()) {
            intent.putExtra("url", url)
        }
        
        if (!targetTab.isNullOrEmpty()) {
            intent.putExtra("target_tab", targetTab)
        } else if (!url.isNullOrEmpty()) {
            // Default to news if URL is present but no target tab specified
            intent.putExtra("target_tab", "news")
        }

        val pendingIntent = PendingIntent.getActivity(
            this, System.currentTimeMillis().toInt(), intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val finalChannelId = channelId ?: "f1_updates_channel_v3"
        val soundUri = android.net.Uri.parse("android.resource://" + packageName + "/" + R.raw.notification_sound)
        
        // Use a generic icon if the small one is problematic, but for now stick to small
        // Ensure it's the one we expect.
        val largeIcon = android.graphics.BitmapFactory.decodeResource(resources, R.drawable.ic_notification_large)

        // Determine priority based on channel
        val priority = when (finalChannelId) {
            "f1_nuclear" -> NotificationCompat.PRIORITY_MAX
            "f1_major" -> NotificationCompat.PRIORITY_HIGH
            else -> NotificationCompat.PRIORITY_DEFAULT
        }

        val notificationBuilder = NotificationCompat.Builder(this, finalChannelId)
            .setSmallIcon(R.drawable.ic_notification_small)
            .setLargeIcon(largeIcon)
            .setColor(android.graphics.Color.parseColor("#FF0080")) // F1 Pink/Red
            .setContentTitle(title ?: "F1 Update")
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setSound(soundUri)
            .setContentIntent(pendingIntent)
            .setPriority(priority)
            
        // Handle Image
        if (!imageUrl.isNullOrEmpty()) {
            try {
                val urlObj = java.net.URL(imageUrl)
                val image = android.graphics.BitmapFactory.decodeStream(urlObj.openConnection().getInputStream())
                notificationBuilder.setStyle(NotificationCompat.BigPictureStyle()
                    .bigPicture(image)
                    .setSummaryText(messageBody) // Keep text visible when expanded
                    .bigLargeIcon(null as android.graphics.Bitmap?)) // Hide large icon when expanded
            } catch (e: Exception) {
                e.printStackTrace()
                notificationBuilder.setStyle(NotificationCompat.BigTextStyle().bigText(messageBody))
            }
        } else {
            notificationBuilder.setStyle(NotificationCompat.BigTextStyle().bigText(messageBody))
        }

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Since android Oreo notification channel is needed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val audioAttributes = android.media.AudioAttributes.Builder()
                .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION)
                .build()

            val channelName = when (finalChannelId) {
                "f1_nuclear" -> "Nuclear Updates"
                "f1_major" -> "Major Updates"
                "f1_digest" -> "Daily Digest"
                "f1_app_updates" -> "App Updates"
                else -> "F1 Updates"
            }

            val importance = when (finalChannelId) {
                "f1_nuclear" -> NotificationManager.IMPORTANCE_HIGH
                "f1_major" -> NotificationManager.IMPORTANCE_HIGH
                else -> NotificationManager.IMPORTANCE_DEFAULT
            }

            val channel = NotificationChannel(
                finalChannelId,
                channelName,
                importance
            ).apply {
                setSound(soundUri, audioAttributes)
                enableLights(true)
                lightColor = android.graphics.Color.RED
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Use unique ID to prevent overwriting
        val notificationId = System.currentTimeMillis().toInt()
        android.util.Log.d("FCM", "Generating notification with ID: $notificationId")
        notificationManager.notify(notificationId, notificationBuilder.build())
    }
}
