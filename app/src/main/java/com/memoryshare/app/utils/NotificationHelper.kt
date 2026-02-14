package com.memoryshare.app.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.memoryshare.app.MainActivity
import com.memoryshare.app.R
import com.memoryshare.app.services.MemoryShareMessagingService

/**
 * Helper pour envoyer des notifications locales depuis les repositories
 * quand des événements temps réel sont détectés via Firestore
 */
object NotificationHelper {
    private const val TAG = "NotificationHelper"

    fun initialize(context: Context) {
        createChannels(context)
    }

    private fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val sharedSpacesChannel = NotificationChannel(
                MemoryShareMessagingService.CHANNEL_SHARED_SPACES,
                "Espaces partagés",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications pour les espaces partagés"
                enableLights(true)
            }

            val messagesChannel = NotificationChannel(
                MemoryShareMessagingService.CHANNEL_MESSAGES,
                "Messages",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications de nouveaux messages"
                enableLights(true)
                enableVibration(true)
            }

            val generalChannel = NotificationChannel(
                MemoryShareMessagingService.CHANNEL_GENERAL,
                "Général",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications générales"
            }

            nm.createNotificationChannels(listOf(sharedSpacesChannel, messagesChannel, generalChannel))
        }
    }

    /**
     * Notification quand un média est ajouté dans un espace partagé
     */
    fun notifyNewMediaInSpace(context: Context, spaceName: String, uploaderName: String, spaceId: String) {
        showNotification(
            context = context,
            channelId = MemoryShareMessagingService.CHANNEL_SHARED_SPACES,
            title = spaceName,
            body = "$uploaderName a ajouté un nouveau souvenir",
            notificationId = "space_media_$spaceId".hashCode()
        )
    }

    /**
     * Notification quand l'utilisateur est invité dans un espace partagé
     */
    fun notifySpaceInvitation(context: Context, spaceName: String, inviterName: String, spaceId: String) {
        showNotification(
            context = context,
            channelId = MemoryShareMessagingService.CHANNEL_SHARED_SPACES,
            title = "Invitation",
            body = "$inviterName vous a invité dans \"$spaceName\"",
            notificationId = "space_invite_$spaceId".hashCode()
        )
    }

    /**
     * Notification pour un nouveau message reçu
     */
    fun notifyNewMessage(context: Context, senderName: String, messagePreview: String, conversationId: String) {
        showNotification(
            context = context,
            channelId = MemoryShareMessagingService.CHANNEL_MESSAGES,
            title = senderName,
            body = messagePreview,
            notificationId = "message_$conversationId".hashCode(),
            priority = NotificationCompat.PRIORITY_HIGH
        )
    }

    /**
     * Notification pour une nouvelle publication d'un utilisateur suivi
     */
    fun notifyNewPost(context: Context, authorName: String, postPreview: String?) {
        showNotification(
            context = context,
            channelId = MemoryShareMessagingService.CHANNEL_GENERAL,
            title = "Nouvelle publication",
            body = if (postPreview != null) "$authorName : $postPreview" else "$authorName a publié quelque chose",
            notificationId = "post_${System.currentTimeMillis()}".hashCode()
        )
    }

    private fun showNotification(
        context: Context,
        channelId: String,
        title: String,
        body: String,
        notificationId: Int,
        priority: Int = NotificationCompat.PRIORITY_DEFAULT
    ) {
        try {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context, notificationId, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(body)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setPriority(priority)
                .build()

            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.notify(notificationId, notification)
            Log.d(TAG, "Notification shown: $title - $body")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show notification", e)
        }
    }
}
