package com.memoryshare.app.services.firebase

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.firebase.Firebase
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID

/**
 * Firebase Storage Manager
 * Handles all file upload/download operations
 */
class FirebaseStorageManager(private val context: Context) {

    private val storage = Firebase.storage
    private val storageRef: StorageReference = storage.reference

    // Storage paths
    companion object {
        const val PROFILE_PICTURES = "profile_pictures"
        const val CHAT_IMAGES = "chat_images"
        const val CHAT_VIDEOS = "chat_videos"
        const val CHAT_AUDIO = "chat_audio"
        const val CHAT_FILES = "chat_files"
        const val POST_MEDIA = "post_media"
        const val STORY_MEDIA = "story_media"
        const val REEL_VIDEOS = "reel_videos"
        const val SHARED_SPACE_MEDIA = "shared_space_media"

        // Compression settings
        const val MAX_IMAGE_DIMENSION = 1920
        const val JPEG_QUALITY = 85
        const val THUMBNAIL_SIZE = 200
    }

    // ==================== UPLOAD OPERATIONS ====================

    /**
     * Upload profile picture
     */
    suspend fun uploadProfilePicture(userId: String, imageUri: Uri): Result<String> {
        return uploadImage(
            uri = imageUri,
            path = "$PROFILE_PICTURES/$userId.jpg",
            compress = true
        )
    }

    /**
     * Upload chat image
     */
    suspend fun uploadChatImage(
        conversationId: String,
        imageUri: Uri,
        compress: Boolean = true
    ): Result<UploadResult> {
        val fileName = "${UUID.randomUUID()}.jpg"
        val path = "$CHAT_IMAGES/$conversationId/$fileName"
        val thumbnailPath = "$CHAT_IMAGES/$conversationId/thumb_$fileName"

        return try {
            // Upload main image
            val imageResult = uploadImage(uri = imageUri, path = path, compress = compress)
            if (imageResult.isFailure) {
                return Result.failure(imageResult.exceptionOrNull() ?: Exception("Upload failed"))
            }

            // Upload thumbnail
            val thumbnailResult = uploadThumbnail(imageUri, thumbnailPath)

            Result.success(
                UploadResult(
                    url = imageResult.getOrThrow(),
                    thumbnailUrl = thumbnailResult.getOrNull(),
                    fileName = fileName
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Upload chat video
     */
    suspend fun uploadChatVideo(
        conversationId: String,
        videoUri: Uri
    ): Result<UploadResult> {
        val fileName = "${UUID.randomUUID()}.mp4"
        val path = "$CHAT_VIDEOS/$conversationId/$fileName"

        return try {
            val url = uploadFile(videoUri, path)
            Result.success(
                UploadResult(
                    url = url,
                    thumbnailUrl = null,
                    fileName = fileName
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Upload chat audio
     */
    suspend fun uploadChatAudio(
        conversationId: String,
        audioUri: Uri
    ): Result<UploadResult> {
        val fileName = "${UUID.randomUUID()}.m4a"
        val path = "$CHAT_AUDIO/$conversationId/$fileName"

        return try {
            val url = uploadFile(audioUri, path)
            Result.success(
                UploadResult(
                    url = url,
                    thumbnailUrl = null,
                    fileName = fileName
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Upload chat file
     */
    suspend fun uploadChatFile(
        conversationId: String,
        fileUri: Uri,
        originalFileName: String
    ): Result<UploadResult> {
        val extension = originalFileName.substringAfterLast('.', "")
        val fileName = "${UUID.randomUUID()}.$extension"
        val path = "$CHAT_FILES/$conversationId/$fileName"

        return try {
            val url = uploadFile(fileUri, path)
            Result.success(
                UploadResult(
                    url = url,
                    thumbnailUrl = null,
                    fileName = originalFileName
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Upload post media
     */
    suspend fun uploadPostMedia(
        userId: String,
        mediaUri: Uri,
        isVideo: Boolean
    ): Result<UploadResult> {
        val extension = if (isVideo) "mp4" else "jpg"
        val fileName = "${UUID.randomUUID()}.$extension"
        val path = "$POST_MEDIA/$userId/$fileName"

        return try {
            val url = if (isVideo) {
                uploadFile(mediaUri, path)
            } else {
                uploadImage(uri = mediaUri, path = path, compress = true).getOrThrow()
            }
            Result.success(
                UploadResult(
                    url = url,
                    thumbnailUrl = null,
                    fileName = fileName
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Upload story media
     */
    suspend fun uploadStoryMedia(
        userId: String,
        mediaUri: Uri,
        isVideo: Boolean
    ): Result<UploadResult> {
        val extension = if (isVideo) "mp4" else "jpg"
        val fileName = "${UUID.randomUUID()}.$extension"
        val path = "$STORY_MEDIA/$userId/$fileName"

        return try {
            val url = if (isVideo) {
                uploadFile(mediaUri, path)
            } else {
                uploadImage(uri = mediaUri, path = path, compress = true).getOrThrow()
            }
            Result.success(
                UploadResult(
                    url = url,
                    thumbnailUrl = null,
                    fileName = fileName
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Upload reel video
     */
    suspend fun uploadReelVideo(userId: String, videoUri: Uri): Result<UploadResult> {
        val fileName = "${UUID.randomUUID()}.mp4"
        val path = "$REEL_VIDEOS/$userId/$fileName"

        return try {
            val url = uploadFile(videoUri, path)
            Result.success(
                UploadResult(
                    url = url,
                    thumbnailUrl = null,
                    fileName = fileName
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Upload shared space media
     */
    suspend fun uploadSharedSpaceMedia(
        spaceId: String,
        mediaUri: Uri,
        isVideo: Boolean
    ): Result<UploadResult> {
        val extension = if (isVideo) "mp4" else "jpg"
        val fileName = "${UUID.randomUUID()}.$extension"
        val path = "$SHARED_SPACE_MEDIA/$spaceId/$fileName"
        val thumbnailPath = "$SHARED_SPACE_MEDIA/$spaceId/thumb_$fileName"

        return try {
            val url = if (isVideo) {
                uploadFile(mediaUri, path)
            } else {
                uploadImage(uri = mediaUri, path = path, compress = true).getOrThrow()
            }

            // Generate thumbnail for images
            val thumbnailUrl = if (!isVideo) {
                uploadThumbnail(mediaUri, thumbnailPath).getOrNull()
            } else null

            Result.success(
                UploadResult(
                    url = url,
                    thumbnailUrl = thumbnailUrl,
                    fileName = fileName
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== CORE UPLOAD METHODS ====================

    /**
     * Upload image with optional compression
     */
    private suspend fun uploadImage(
        uri: Uri,
        path: String,
        compress: Boolean = true
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val bytes = if (compress) {
                compressImage(uri)
            } else {
                context.contentResolver.openInputStream(uri)?.readBytes()
                    ?: throw Exception("Cannot read file")
            }

            val ref = storageRef.child(path)
            ref.putBytes(bytes).await()
            val downloadUrl = ref.downloadUrl.await()

            Result.success(downloadUrl.toString())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Upload thumbnail
     */
    private suspend fun uploadThumbnail(uri: Uri, path: String): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val bytes = createThumbnail(uri)
                val ref = storageRef.child(path)
                ref.putBytes(bytes).await()
                val downloadUrl = ref.downloadUrl.await()
                Result.success(downloadUrl.toString())
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * Upload file without processing
     */
    private suspend fun uploadFile(uri: Uri, path: String): String =
        withContext(Dispatchers.IO) {
            val ref = storageRef.child(path)
            ref.putFile(uri).await()
            ref.downloadUrl.await().toString()
        }

    /**
     * Upload bytes directly
     */
    suspend fun uploadBytes(bytes: ByteArray, path: String): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val ref = storageRef.child(path)
                ref.putBytes(bytes).await()
                val downloadUrl = ref.downloadUrl.await()
                Result.success(downloadUrl.toString())
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    // ==================== IMAGE PROCESSING ====================

    /**
     * Compress image
     */
    private fun compressImage(uri: Uri): ByteArray {
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw Exception("Cannot open image")

        val originalBitmap = BitmapFactory.decodeStream(inputStream)
        inputStream.close()

        // Calculate scale
        val scale = minOf(
            MAX_IMAGE_DIMENSION.toFloat() / originalBitmap.width,
            MAX_IMAGE_DIMENSION.toFloat() / originalBitmap.height,
            1f
        )

        val scaledBitmap = if (scale < 1f) {
            val newWidth = (originalBitmap.width * scale).toInt()
            val newHeight = (originalBitmap.height * scale).toInt()
            Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, true).also {
                if (it != originalBitmap) originalBitmap.recycle()
            }
        } else {
            originalBitmap
        }

        val outputStream = ByteArrayOutputStream()
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, outputStream)
        scaledBitmap.recycle()

        return outputStream.toByteArray()
    }

    /**
     * Create thumbnail
     */
    private fun createThumbnail(uri: Uri): ByteArray {
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw Exception("Cannot open image")

        val originalBitmap = BitmapFactory.decodeStream(inputStream)
        inputStream.close()

        // Calculate scale for thumbnail
        val scale = minOf(
            THUMBNAIL_SIZE.toFloat() / originalBitmap.width,
            THUMBNAIL_SIZE.toFloat() / originalBitmap.height
        )

        val newWidth = (originalBitmap.width * scale).toInt()
        val newHeight = (originalBitmap.height * scale).toInt()

        val thumbnailBitmap = Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, true)
        if (thumbnailBitmap != originalBitmap) originalBitmap.recycle()

        val outputStream = ByteArrayOutputStream()
        thumbnailBitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
        thumbnailBitmap.recycle()

        return outputStream.toByteArray()
    }

    // ==================== DELETE OPERATIONS ====================

    /**
     * Delete file
     */
    suspend fun deleteFile(path: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            storageRef.child(path).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Delete file by URL
     */
    suspend fun deleteFileByUrl(url: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            storage.getReferenceFromUrl(url).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== DOWNLOAD OPERATIONS ====================

    /**
     * Download file to local storage
     */
    suspend fun downloadFile(url: String, localFile: File): Result<File> =
        withContext(Dispatchers.IO) {
            try {
                storage.getReferenceFromUrl(url).getFile(localFile).await()
                Result.success(localFile)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * Get download URL for path
     */
    suspend fun getDownloadUrl(path: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val url = storageRef.child(path).downloadUrl.await()
            Result.success(url.toString())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== DATA CLASSES ====================

    data class UploadResult(
        val url: String,
        val thumbnailUrl: String?,
        val fileName: String
    )
}
