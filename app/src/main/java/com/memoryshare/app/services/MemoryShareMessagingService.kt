package com.memoryshare.app.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import androidx.core.graphics.drawable.IconCompat
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.memoryshare.app.MainActivity
import com.memoryshare.app.MemoryShareApplication
import com.memoryshare.app.R
import com.memoryshare.app.data.model.Message
import com.memoryshare.app.data.model.MessageStatus
import com.memoryshare.app.data.model.MessageType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Firebase Cloud Messaging Service
 * Handles push notifications for messages, calls, and other events
 */
class MemoryShareMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "FCMService"

        // Notification channels
        const val CHANNEL_MESSAGES = "memoryshare_messages"
        const val CHANNEL_CALLS = "memoryshare_calls"
        const val CHANNEL_STORIES = "memoryshare_stories"
        const val CHANNEL_SHARED_SPACES = "memoryshare_shared_spaces"
        const val CHANNEL_GENERAL = "memoryshare_general"

        // Notification types
        const val TYPE_MESSAGE = "message"
        const val TYPE_CALL = "call"
        const val TYPE_STORY = "story"
        const val TYPE_FOLLOW = "follow"
        const val TYPE_LIKE = "like"
        const val TYPE_COMMENT = "comment"
        const val TYPE_SPACE_MEDIA = "space_media"
        const val TYPE_SPACE_INVITE = "space_invite"
        const val TYPE_NEW_POST = "new_post"

        // Data keys
        const val KEY_TYPE = "type"
        const val KEY_TITLE = "title"
        const val KEY_BODY = "body"
        const val KEY_SENDER_ID = "senderId"
        const val KEY_SENDER_NAME = "senderName"
        const val KEY_SENDER_IMAGE = "senderImage"
        const val KEY_CONVERSATION_ID = "conversationId"
        const val KEY_MESSAGE_ID = "messageId"
        const val KEY_CALL_ID = "callId"
        const val KEY_CALL_TYPE = "callType"

        // Notification IDs
        const val CALL_NOTIFICATION_ID = 1001
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    /**
     * Called when a new FCM token is generated
     */
    override fun onNewToken(token: String) {
        Log.d(TAG, "New FCM token received")
        saveTokenToServer(token)
    }

    /**
     * Called when a message is received
     */
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(TAG, "Message received from: ${remoteMessage.from}")

        // Handle data payload
        if (remoteMessage.data.isNotEmpty()) {
            handleDataMessage(remoteMessage.data)
        }

        // Handle notification payload (when app is in foreground)
        remoteMessage.notification?.let { notification ->
            showSimpleNotification(
                title = notification.title ?: "MemoryShare",
                body = notification.body ?: "",
                channelId = CHANNEL_GENERAL
            )
        }
    }

    /**
     * Handle data message
     */
    private fun handleDataMessage(data: Map<String, String>) {
        val type = data[KEY_TYPE] ?: return

        when (type) {
            TYPE_MESSAGE -> handleMessageNotification(data)
            TYPE_CALL -> handleCallNotification(data)
            TYPE_STORY -> handleStoryNotification(data)
            TYPE_FOLLOW -> handleFollowNotification(data)
            TYPE_LIKE -> handleLikeNotification(data)
            TYPE_COMMENT -> handleCommentNotification(data)
            TYPE_SPACE_MEDIA -> handleSpaceMediaNotification(data)
            TYPE_SPACE_INVITE -> handleSpaceInviteNotification(data)
            TYPE_NEW_POST -> handleNewPostNotification(data)
            else -> {
                Log.w(TAG, "Unknown notification type: $type")
            }
        }
    }

    /**
     * Handle new message notification
     */
    private fun handleMessageNotification(data: Map<String, String>) {
        val senderName = data[KEY_SENDER_NAME] ?: "Someone"
        val senderImage = data[KEY_SENDER_IMAGE]
        val body = data[KEY_BODY] ?: "New message"
        val conversationId = data[KEY_CONVERSATION_ID]

        serviceScope.launch {
            // Load sender image
            val senderIcon = senderImage?.let { loadImageAsBitmap(it) }

            val person = Person.Builder()
                .setName(senderName)
                .apply {
                    senderIcon?.let { setIcon(IconCompat.createWithBitmap(it)) }
                }
                .build()

            // Build messaging style notification
            val messagingStyle = NotificationCompat.MessagingStyle(person)
                .addMessage(body, System.currentTimeMillis(), person)

            // Create intent for opening conversation
            val intent = Intent(this@MemoryShareMessagingService, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("conversationId", conversationId)
            }

            val pendingIntent = PendingIntent.getActivity(
                this@MemoryShareMessagingService,
                conversationId?.hashCode() ?: 0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Reply action
            val replyIntent = createReplyIntent(conversationId)
            val replyAction = NotificationCompat.Action.Builder(
                R.drawable.ic_notification,
                "Reply",
                replyIntent
            ).addRemoteInput(
                androidx.core.app.RemoteInput.Builder("reply_text")
                    .setLabel("Reply")
                    .build()
            ).build()

            val notification = NotificationCompat.Builder(this@MemoryShareMessagingService, CHANNEL_MESSAGES)
                .setSmallIcon(R.drawable.ic_notification)
                .setStyle(messagingStyle)
                .setContentIntent(pendingIntent)
                .addAction(replyAction)
                .setAutoCancel(true)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .build()

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(conversationId?.hashCode() ?: System.currentTimeMillis().toInt(), notification)
        }
    }

    /**
     * Handle incoming call notification
     */
    private fun handleCallNotification(data: Map<String, String>) {
        val senderName = data[KEY_SENDER_NAME] ?: "Someone"
        val callId = data[KEY_CALL_ID]
        val callType = data[KEY_CALL_TYPE] ?: "voice"
        val isVideo = callType == "video"

        val title = if (isVideo) "Incoming Video Call" else "Incoming Voice Call"

        // Accept call intent
        val acceptIntent = Intent(this, MainActivity::class.java).apply {
            action = "ACCEPT_CALL"
            putExtra("callId", callId)
            putExtra("isVideo", isVideo)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val acceptPendingIntent = PendingIntent.getActivity(
            this, 1, acceptIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Decline call intent
        val declineIntent = Intent(this, CallService::class.java).apply {
            action = "DECLINE_CALL"
            putExtra("callId", callId)
        }
        val declinePendingIntent = PendingIntent.getService(
            this, 2, declineIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_CALLS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText("$senderName is calling...")
            .addAction(R.drawable.ic_notification, "Accept", acceptPendingIntent)
            .addAction(R.drawable.ic_notification, "Decline", declinePendingIntent)
            .setOngoing(true)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setFullScreenIntent(acceptPendingIntent, true)
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(CALL_NOTIFICATION_ID, notification)
    }

    /**
     * Handle new story notification
     */
    private fun handleStoryNotification(data: Map<String, String>) {
        val senderName = data[KEY_SENDER_NAME] ?: "Someone"

        showSimpleNotification(
            title = "New Story",
            body = "$senderName posted a new story",
            channelId = CHANNEL_STORIES
        )
    }

    /**
     * Handle new follower notification
     */
    private fun handleFollowNotification(data: Map<String, String>) {
        val senderName = data[KEY_SENDER_NAME] ?: "Someone"

        showSimpleNotification(
            title = "New Follower",
            body = "$senderName started following you",
            channelId = CHANNEL_GENERAL
        )
    }

    /**
     * Handle like notification
     */
    private fun handleLikeNotification(data: Map<String, String>) {
        val senderName = data[KEY_SENDER_NAME] ?: "Someone"
        val body = data[KEY_BODY] ?: "liked your post"

        showSimpleNotification(
            title = senderName,
            body = body,
            channelId = CHANNEL_GENERAL
        )
    }

    /**
     * Handle comment notification
     */
    private fun handleCommentNotification(data: Map<String, String>) {
        val senderName = data[KEY_SENDER_NAME] ?: "Someone"
        val body = data[KEY_BODY] ?: "commented on your post"

        showSimpleNotification(
            title = senderName,
            body = body,
            channelId = CHANNEL_GENERAL
        )
    }

    /**
     * Handle new media in shared space notification
     */
    private fun handleSpaceMediaNotification(data: Map<String, String>) {
        val senderName = data[KEY_SENDER_NAME] ?: "Quelqu'un"
        val spaceName = data["spaceName"] ?: "un espace partagé"

        showSimpleNotification(
            title = spaceName,
            body = "$senderName a ajouté un nouveau média",
            channelId = CHANNEL_SHARED_SPACES
        )
    }

    /**
     * Handle shared space invitation notification
     */
    private fun handleSpaceInviteNotification(data: Map<String, String>) {
        val senderName = data[KEY_SENDER_NAME] ?: "Quelqu'un"
        val spaceName = data["spaceName"] ?: "un espace partagé"

        showSimpleNotification(
            title = "Invitation",
            body = "$senderName vous a invité dans \"$spaceName\"",
            channelId = CHANNEL_SHARED_SPACES
        )
    }

    /**
     * Handle new post notification from followed user
     */
    private fun handleNewPostNotification(data: Map<String, String>) {
        val senderName = data[KEY_SENDER_NAME] ?: "Quelqu'un"

        showSimpleNotification(
            title = "Nouvelle publication",
            body = "$senderName a publié quelque chose",
            channelId = CHANNEL_GENERAL
        )
    }

    /**
     * Show simple notification
     */
    private fun showSimpleNotification(title: String, body: String, channelId: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    /**
     * Create notification channels
     */
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Messages channel
            val messagesChannel = NotificationChannel(
                CHANNEL_MESSAGES,
                "Messages",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "New message notifications"
                enableLights(true)
                enableVibration(true)
            }

            // Calls channel
            val callsChannel = NotificationChannel(
                CHANNEL_CALLS,
                "Calls",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Incoming call notifications"
                enableLights(true)
                enableVibration(true)
                setSound(
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE),
                    audioAttributes
                )
            }

            // Stories channel
            val storiesChannel = NotificationChannel(
                CHANNEL_STORIES,
                "Stories",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Story notifications"
            }

            // Shared spaces channel
            val sharedSpacesChannel = NotificationChannel(
                CHANNEL_SHARED_SPACES,
                "Espaces partagés",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications pour les espaces partagés (médias, invitations)"
                enableLights(true)
            }

            // General channel
            val generalChannel = NotificationChannel(
                CHANNEL_GENERAL,
                "General",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "General notifications"
            }

            notificationManager.createNotificationChannels(
                listOf(messagesChannel, callsChannel, storiesChannel, sharedSpacesChannel, generalChannel)
            )
        }
    }

    /**
     * Create reply intent for direct reply
     */
    private fun createReplyIntent(conversationId: String?): PendingIntent {
        val intent = Intent(this, DirectReplyReceiver::class.java).apply {
            putExtra("conversationId", conversationId)
        }
        return PendingIntent.getBroadcast(
            this,
            conversationId?.hashCode() ?: 0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }

    /**
     * Load image as bitmap
     */
    private suspend fun loadImageAsBitmap(url: String): android.graphics.Bitmap? {
        return try {
            val loader = ImageLoader(this)
            val request = ImageRequest.Builder(this)
                .data(url)
                .allowHardware(false)
                .build()
            val result = loader.execute(request)
            (result as? SuccessResult)?.drawable?.let { drawable ->
                val bitmap = android.graphics.Bitmap.createBitmap(
                    drawable.intrinsicWidth,
                    drawable.intrinsicHeight,
                    android.graphics.Bitmap.Config.ARGB_8888
                )
                val canvas = android.graphics.Canvas(bitmap)
                drawable.setBounds(0, 0, canvas.width, canvas.height)
                drawable.draw(canvas)
                bitmap
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load image", e)
            null
        }
    }

    /**
     * Save FCM token to Firestore for the current user
     */
    private fun saveTokenToServer(token: String) {
        val userId = Firebase.auth.currentUser?.uid
        if (userId == null) {
            Log.w(TAG, "Cannot save FCM token: no authenticated user")
            return
        }
        serviceScope.launch {
            try {
                val app = applicationContext as? MemoryShareApplication
                app?.firestoreManager?.saveFcmToken(userId, token)
                Log.d(TAG, "FCM token saved for user $userId")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save FCM token", e)
            }
        }
    }
}

/**
 * Broadcast receiver for direct reply from notification.
 * Sends the reply message via Firestore and dismisses the notification.
 */
class DirectReplyReceiver : android.content.BroadcastReceiver() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val conversationId = intent.getStringExtra("conversationId") ?: return
        val remoteInput = androidx.core.app.RemoteInput.getResultsFromIntent(intent)
        val replyText = remoteInput?.getCharSequence("reply_text")?.toString() ?: return

        val currentUserId = Firebase.auth.currentUser?.uid ?: return

        val pendingResult = goAsync()

        scope.launch {
            try {
                val app = context.applicationContext as? MemoryShareApplication ?: return@launch
                val message = Message(
                    id = UUID.randomUUID().toString(),
                    conversationId = conversationId,
                    senderId = currentUserId,
                    content = replyText,
                    type = MessageType.TEXT,
                    timestamp = System.currentTimeMillis(),
                    status = MessageStatus.SENT
                )
                app.firestoreManager.sendMessage(conversationId, message)

                // Update notification to show reply was sent
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                val updatedNotification = NotificationCompat.Builder(context, MemoryShareMessagingService.CHANNEL_MESSAGES)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentText("Reply sent")
                    .build()
                notificationManager.notify(conversationId.hashCode(), updatedNotification)

                Log.d("DirectReplyReceiver", "Reply sent to $conversationId")
            } catch (e: Exception) {
                Log.e("DirectReplyReceiver", "Failed to send reply", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
