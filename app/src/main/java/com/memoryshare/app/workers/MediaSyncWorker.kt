package com.memoryshare.app.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.memoryshare.app.data.local.AppDatabase
import com.memoryshare.app.utils.MediaSyncManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Worker pour synchroniser les médias en arrière-plan
 * - Upload des médias non synchronisés vers Firebase
 * - Nettoyage des médias anciens du cache local
 */
class MediaSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "MediaSyncWorker"
        const val WORK_NAME = "media_sync_work"

        // Paramètres de configuration
        const val PARAM_CLEANUP_DAYS = "cleanup_days"
        const val DEFAULT_CLEANUP_DAYS = 30
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting media sync work...")

            // Initialiser MediaSyncManager
            val database = AppDatabase.getDatabase(applicationContext)
            val mediaSyncManager = MediaSyncManager(
                context = applicationContext,
                mediaCacheDao = database.mediaCacheDao(),
                database = database
            )

            // 1. Upload des médias en attente
            Log.d(TAG, "Uploading pending media...")
            val uploadResult = mediaSyncManager.uploadPendingMedia()

            if (uploadResult.isSuccess) {
                val uploadedCount = uploadResult.getOrDefault(0)
                Log.d(TAG, "Successfully uploaded $uploadedCount pending media files")
            } else {
                Log.e(TAG, "Failed to upload pending media: ${uploadResult.exceptionOrNull()?.message}")
                // Ne pas échouer complètement si l'upload échoue, continuer avec le nettoyage
            }

            // 2. Nettoyage des médias anciens
            val cleanupDays = inputData.getInt(PARAM_CLEANUP_DAYS, DEFAULT_CLEANUP_DAYS)
            Log.d(TAG, "Cleaning up media older than $cleanupDays days...")
            val cleanedCount = mediaSyncManager.cleanupOldMedia(cleanupDays)
            Log.d(TAG, "Cleaned up $cleanedCount old media files")

            // 3. TODO: Télécharger les nouveaux médias depuis Firebase
            // Cela nécessiterait un système de notifications push ou de polling
            // pour savoir quels médias sont nouveaux

            Log.d(TAG, "Media sync work completed successfully")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Media sync work failed", e)
            // Retry en cas d'erreur réseau ou temporaire
            if (runAttemptCount < 3) {
                Log.d(TAG, "Retrying... (attempt ${runAttemptCount + 1}/3)")
                Result.retry()
            } else {
                Log.e(TAG, "Max retry attempts reached, failing")
                Result.failure()
            }
        }
    }
}
