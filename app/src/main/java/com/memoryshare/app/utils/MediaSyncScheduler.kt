package com.memoryshare.app.utils

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.memoryshare.app.workers.MediaSyncWorker
import java.util.concurrent.TimeUnit

/**
 * Gestionnaire de planification pour la synchronisation des médias en arrière-plan
 */
object MediaSyncScheduler {

    /**
     * Démarre la synchronisation périodique des médias
     * @param context Context de l'application
     * @param intervalHours Intervalle en heures entre chaque synchronisation (défaut: 6h)
     * @param cleanupDays Nombre de jours avant de nettoyer les anciens médias (défaut: 30)
     * @param requiresCharging Si true, ne sync que lors du chargement (défaut: false)
     * @param requiresWifi Si true, ne sync qu'en WiFi (défaut: true pour économiser les données)
     */
    fun startPeriodicSync(
        context: Context,
        intervalHours: Long = 6,
        cleanupDays: Int = MediaSyncWorker.DEFAULT_CLEANUP_DAYS,
        requiresCharging: Boolean = false,
        requiresWifi: Boolean = true
    ) {
        // Contraintes pour l'exécution du worker
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(if (requiresWifi) NetworkType.UNMETERED else NetworkType.CONNECTED)
            .setRequiresCharging(requiresCharging)
            .setRequiresBatteryNotLow(true)
            .build()

        // Données d'entrée pour le worker
        val inputData = workDataOf(
            MediaSyncWorker.PARAM_CLEANUP_DAYS to cleanupDays
        )

        // Créer la requête de travail périodique
        val syncWorkRequest = PeriodicWorkRequestBuilder<MediaSyncWorker>(
            intervalHours, TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .setInputData(inputData)
            .addTag("media_sync")
            .build()

        // Planifier le travail (remplace la précédente planification si elle existe)
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            MediaSyncWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP, // Garder la planification existante si elle fonctionne
            syncWorkRequest
        )
    }

    /**
     * Arrête la synchronisation périodique des médias
     */
    fun stopPeriodicSync(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(MediaSyncWorker.WORK_NAME)
    }

    /**
     * Force une synchronisation immédiate (one-time)
     */
    fun syncNow(
        context: Context,
        cleanupDays: Int = MediaSyncWorker.DEFAULT_CLEANUP_DAYS
    ) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val inputData = workDataOf(
            MediaSyncWorker.PARAM_CLEANUP_DAYS to cleanupDays
        )

        val syncWorkRequest = androidx.work.OneTimeWorkRequestBuilder<MediaSyncWorker>()
            .setConstraints(constraints)
            .setInputData(inputData)
            .addTag("media_sync_immediate")
            .build()

        WorkManager.getInstance(context).enqueue(syncWorkRequest)
    }

    /**
     * Vérifie si la synchronisation périodique est active
     */
    fun isSyncScheduled(context: Context): Boolean {
        val workInfos = WorkManager.getInstance(context)
            .getWorkInfosForUniqueWork(MediaSyncWorker.WORK_NAME)
            .get()
        return workInfos.isNotEmpty() && workInfos.any { !it.state.isFinished }
    }
}
