package com.memoryshare.app.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import com.memoryshare.app.data.local.dao.MediaCacheDao
import com.memoryshare.app.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.URL
import java.util.UUID

/**
 * Gestionnaire de synchronisation des médias entre cache local et Firebase
 */
class MediaSyncManager(
    private val context: Context,
    private val mediaCacheDao: MediaCacheDao,
    private val compressionManager: MediaCompressionManager,
    private val storageManager: FirebaseStorageManager
) {

    private val _uploadProgress = MutableStateFlow<Float?>(null)
    val uploadProgress: StateFlow<Float?> = _uploadProgress.asStateFlow()

    private val _downloadProgress = MutableStateFlow<Float?>(null)
    val downloadProgress: StateFlow<Float?> = _downloadProgress.asStateFlow()

    companion object {
        private const val TAG = "MediaSyncManager"
    }

    /**
     * Prépare un média pour l'upload (compression + cache local)
     * @return MediaCache prêt à être uploadé
     */
    suspend fun prepareMediaForUpload(
        uri: Uri,
        type: MediaType,
        quality: MediaQuality,
        sourceType: MediaSourceType,
        sourceId: String,
        uploadedBy: String
    ): Result<MediaCache> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Preparing media for upload: type=$type, quality=$quality")

            // Compresser/copier le média dans le cache
            val compressionResult = when (type) {
                MediaType.IMAGE -> compressionManager.compressImage(uri, quality)
                MediaType.VIDEO, MediaType.AUDIO -> compressionManager.cacheMediaFile(uri, type, quality)
            }

            if (compressionResult.isFailure) {
                return@withContext Result.failure(
                    compressionResult.exceptionOrNull() ?: Exception("Compression failed")
                )
            }

            val result = compressionResult.getOrThrow()

            // Créer l'entrée MediaCache
            val mediaCache = MediaCache(
                id = UUID.randomUUID().toString(),
                localPath = result.filePath,
                firebaseUrl = null,  // Sera rempli après l'upload
                type = type,
                quality = quality,
                sourceType = sourceType,
                sourceId = sourceId,
                size = result.size,
                width = result.width,
                height = result.height,
                isSynced = false,
                isDownloaded = true,
                uploadedBy = uploadedBy
            )

            // Sauvegarder dans la DB
            mediaCacheDao.insert(mediaCache)

            Log.d(TAG, "Media prepared successfully: ${mediaCache.id}, size=${result.size / 1024}KB")

            Result.success(mediaCache)
        } catch (e: Exception) {
            Log.e(TAG, "Error preparing media", e)
            Result.failure(e)
        }
    }

    /**
     * Upload un média vers Firebase Storage
     */
    suspend fun uploadMedia(mediaCache: MediaCache): Result<String> = withContext(Dispatchers.IO) {
        try {
            _uploadProgress.value = 0f

            val file = File(mediaCache.localPath)
            if (!file.exists()) {
                return@withContext Result.failure(IOException("Fichier local introuvable"))
            }

            val uri = file.toUri()

            // Upload vers Firebase selon le type
            val uploadResult = when (mediaCache.type) {
                MediaType.IMAGE -> storageManager.uploadImage(uri)
                MediaType.VIDEO -> storageManager.uploadVideo(uri)
                MediaType.AUDIO -> storageManager.uploadAudio(uri)
            }

            if (uploadResult.isFailure) {
                _uploadProgress.value = null
                return@withContext uploadResult
            }

            val firebaseUrl = uploadResult.getOrThrow()

            // Mettre à jour la DB avec l'URL Firebase
            mediaCacheDao.markAsSynced(mediaCache.id, firebaseUrl)

            _uploadProgress.value = 1f
            Log.d(TAG, "Media uploaded successfully: ${mediaCache.id}")

            _uploadProgress.value = null
            Result.success(firebaseUrl)
        } catch (e: Exception) {
            _uploadProgress.value = null
            Log.e(TAG, "Error uploading media", e)
            Result.failure(e)
        }
    }

    /**
     * Upload tous les médias non synchronisés
     */
    suspend fun uploadPendingMedia(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val unsyncedMedia = mediaCacheDao.getUnsyncedMedia()
            var successCount = 0

            for (media in unsyncedMedia) {
                val result = uploadMedia(media)
                if (result.isSuccess) {
                    successCount++
                }
            }

            Log.d(TAG, "Uploaded $successCount/${unsyncedMedia.size} pending media")
            Result.success(successCount)
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading pending media", e)
            Result.failure(e)
        }
    }

    /**
     * Télécharge un média depuis Firebase vers le cache local
     */
    suspend fun downloadMedia(
        firebaseUrl: String,
        type: MediaType,
        quality: MediaQuality,
        sourceType: MediaSourceType,
        sourceId: String,
        uploadedBy: String
    ): Result<MediaCache> = withContext(Dispatchers.IO) {
        try {
            _downloadProgress.value = 0f

            // Vérifier si déjà en cache
            val existingCache = mediaCacheDao.getMediaByFirebaseUrl(firebaseUrl)
            if (existingCache != null && File(existingCache.localPath).exists()) {
                _downloadProgress.value = null
                Log.d(TAG, "Media already in cache: ${existingCache.id}")
                return@withContext Result.success(existingCache)
            }

            // Télécharger depuis Firebase
            val cacheDir = File(context.cacheDir, "media_cache")
            if (!cacheDir.exists()) {
                cacheDir.mkdirs()
            }

            val extension = when (type) {
                MediaType.IMAGE -> "jpg"
                MediaType.VIDEO -> "mp4"
                MediaType.AUDIO -> "mp3"
            }

            val outputFile = File(cacheDir, "${UUID.randomUUID()}.$extension")

            // Télécharger le fichier
            val url = URL(firebaseUrl)
            url.openStream().use { input ->
                FileOutputStream(outputFile).use { output ->
                    val buffer = ByteArray(4096)
                    var bytesRead: Int
                    var totalBytesRead = 0L

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalBytesRead += bytesRead
                    }
                }
            }

            // Créer l'entrée cache
            val mediaCache = MediaCache(
                id = existingCache?.id ?: UUID.randomUUID().toString(),
                localPath = outputFile.absolutePath,
                firebaseUrl = firebaseUrl,
                type = type,
                quality = quality,
                sourceType = sourceType,
                sourceId = sourceId,
                size = outputFile.length(),
                isSynced = true,
                isDownloaded = true,
                uploadedBy = uploadedBy
            )

            // Sauvegarder dans la DB
            mediaCacheDao.insert(mediaCache)

            _downloadProgress.value = 1f
            Log.d(TAG, "Media downloaded successfully: ${mediaCache.id}, size=${outputFile.length() / 1024}KB")

            _downloadProgress.value = null
            Result.success(mediaCache)
        } catch (e: Exception) {
            _downloadProgress.value = null
            Log.e(TAG, "Error downloading media", e)
            Result.failure(e)
        }
    }

    /**
     * Récupère un média (depuis le cache local ou télécharge depuis Firebase)
     */
    suspend fun getMedia(
        firebaseUrl: String,
        type: MediaType,
        quality: MediaQuality,
        sourceType: MediaSourceType,
        sourceId: String,
        uploadedBy: String
    ): Result<MediaCache> {
        // Vérifier le cache
        val cached = mediaCacheDao.getMediaByFirebaseUrl(firebaseUrl)
        if (cached != null && File(cached.localPath).exists()) {
            return Result.success(cached)
        }

        // Télécharger si pas en cache
        return downloadMedia(firebaseUrl, type, quality, sourceType, sourceId, uploadedBy)
    }

    /**
     * Obtient le chemin local d'un média (télécharge si nécessaire)
     */
    suspend fun getLocalPath(firebaseUrl: String): String? {
        val cached = mediaCacheDao.getMediaByFirebaseUrl(firebaseUrl)
        if (cached != null && File(cached.localPath).exists()) {
            return cached.localPath
        }
        return null  // Retourner null si pas en cache (l'app utilisera l'URL Firebase)
    }

    /**
     * Flow des médias d'une source spécifique
     */
    fun getMediaBySource(sourceType: MediaSourceType, sourceId: String): Flow<List<MediaCache>> {
        return mediaCacheDao.getMediaBySource(sourceType, sourceId)
    }

    /**
     * Nettoie le cache et la DB
     */
    suspend fun cleanupOldMedia(olderThanDays: Int = 30): Int = withContext(Dispatchers.IO) {
        val timestamp = System.currentTimeMillis() - (olderThanDays * 24 * 60 * 60 * 1000L)
        mediaCacheDao.deleteOlderThan(timestamp)
        compressionManager.cleanCache()
    }

    /**
     * Obtient la taille totale du cache
     */
    suspend fun getCacheSize(): Long {
        return mediaCacheDao.getTotalCacheSize() ?: 0L
    }
}
