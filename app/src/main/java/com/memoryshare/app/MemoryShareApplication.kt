package com.memoryshare.app

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.memoryshare.app.data.local.AppDatabase
import com.memoryshare.app.services.calls.AgoraManager
import com.memoryshare.app.services.firebase.FirebaseAuthManager
import com.memoryshare.app.services.firebase.FirebaseStorageManager
import com.memoryshare.app.services.firebase.FirestoreManager
import com.memoryshare.app.services.media.AudioRecorderManager
import com.memoryshare.app.services.media.CameraManager
import com.memoryshare.app.services.realtime.PresenceManager
import com.memoryshare.app.services.realtime.RealtimeSyncManager
import com.memoryshare.app.utils.NotificationHelper

/**
 * Application class
 * Initializes Firebase and all services
 */
class MemoryShareApplication : Application() {

    companion object {
        private const val TAG = "MemoryShareApp"

        // Singleton instance
        lateinit var instance: MemoryShareApplication
            private set
    }

    // Local database
    lateinit var database: AppDatabase
        private set

    // Firebase services
    lateinit var firebaseAuthManager: FirebaseAuthManager
        private set

    lateinit var firestoreManager: FirestoreManager
        private set

    lateinit var firebaseStorageManager: FirebaseStorageManager
        private set

    // Media services
    lateinit var cameraManager: CameraManager
        private set

    lateinit var audioRecorderManager: AudioRecorderManager
        private set

    // Realtime services
    lateinit var presenceManager: PresenceManager
        private set

    lateinit var realtimeSyncManager: RealtimeSyncManager
        private set

    // Call services
    lateinit var agoraManager: AgoraManager
        private set

    // Flag to check if Firebase is initialized
    var isFirebaseInitialized: Boolean = false
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Initialize local database
        database = AppDatabase.getDatabase(this)

        // Initialize Firebase
        initializeFirebase()

        // Initialize services
        initializeServices()

        // Initialize notification channels
        NotificationHelper.initialize(this)

        Log.d(TAG, "MemoryShareApplication initialized")
    }

    /**
     * Initialize Firebase
     */
    private fun initializeFirebase() {
        try {
            FirebaseApp.initializeApp(this)
            isFirebaseInitialized = true
            Log.d(TAG, "Firebase initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Firebase", e)
            isFirebaseInitialized = false
        }
    }

    /**
     * Initialize all services
     */
    private fun initializeServices() {
        // Firebase services
        firebaseAuthManager = FirebaseAuthManager(this)
        firestoreManager = FirestoreManager()
        firebaseStorageManager = FirebaseStorageManager(this)

        // Media services
        cameraManager = CameraManager(this)
        audioRecorderManager = AudioRecorderManager(this)

        // Realtime services
        presenceManager = PresenceManager()
        realtimeSyncManager = RealtimeSyncManager(
            messageDao = database.messageDao(),
            conversationDao = database.conversationDao(),
            context = this
        )

        // Call services
        agoraManager = AgoraManager(this)

        // Initialize presence tracking for current user
        Firebase.auth.currentUser?.uid?.let { userId ->
            presenceManager.initialize(userId)
            realtimeSyncManager.initialize(userId)
        }
    }

    /**
     * Initialize services after user login
     */
    fun onUserLoggedIn(userId: String) {
        presenceManager.initialize(userId)
        realtimeSyncManager.initialize(userId)
        presenceManager.goOnline()
    }

    /**
     * Cleanup on user logout
     */
    fun onUserLoggedOut() {
        presenceManager.cleanup()
        realtimeSyncManager.cleanup()
    }

    override fun onTerminate() {
        super.onTerminate()
        presenceManager.cleanup()
        realtimeSyncManager.cleanup()
        audioRecorderManager.release()
        agoraManager.destroy()
    }
}
