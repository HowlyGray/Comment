package com.memoryshare.app.workers

import android.content.Context
import android.util.Log
import androidx.work.*
import com.memoryshare.app.data.local.AppDatabase
import com.memoryshare.app.services.firebase.FirestoreManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * Worker périodique (toutes les heures) qui :
 * 1. Supprime localement les messages dont expiresAt < maintenant
 * 2. Supprime ces mêmes messages sur Firestore (dans chaque conversation concernée)
 */
class EphemeralMessageCleanupWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "EphemeralCleanupWorker"
        const val WORK_NAME = "ephemeral_message_cleanup"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<EphemeralMessageCleanupWorker>(
                1, TimeUnit.HOURS
            )
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val database = AppDatabase.getDatabase(applicationContext)
            val messageDao = database.messageDao()
            val conversationDao = database.conversationDao()
            val firestoreManager = FirestoreManager()

            // 1. Récupérer les IDs des messages expirés avant de les supprimer
            val expiredIds = messageDao.getExpiredMessageIds()
            if (expiredIds.isEmpty()) {
                Log.d(TAG, "Aucun message éphémère expiré.")
                return@withContext Result.success()
            }

            Log.d(TAG, "${expiredIds.size} message(s) éphémère(s) à supprimer.")

            // 2. Récupérer les conversations concernées pour purger Firestore
            val conversations = conversationDao.getAllConversationsSync()
            conversations.forEach { conv ->
                firestoreManager.deleteExpiredMessages(conv.id)
            }

            // 3. Suppression locale
            messageDao.deleteExpiredMessages()

            Log.d(TAG, "Nettoyage éphémère terminé : ${expiredIds.size} message(s) supprimé(s).")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors du nettoyage des messages éphémères", e)
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }
}
