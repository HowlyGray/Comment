package com.memoryshare.app.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Log
import com.memoryshare.app.data.model.MediaQuality
import com.memoryshare.app.data.model.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID

class MediaCompressionManager(private val context: Context) {

    companion object {
        private const val TAG = "MediaCompression"

        // Paramètres pour images SD
        private const val SD_MAX_WIDTH = 1920
        private const val SD_MAX_HEIGHT = 1080
        private const val SD_JPEG_QUALITY = 80

        // Paramètres pour images HD
        private const val HD_MAX_WIDTH = 3840  // 4K
        private const val HD_MAX_HEIGHT = 2160
        private const val HD_JPEG_QUALITY = 95

        // Limites de taille de fichier
        private const val MAX_SD_SIZE_MB = 5
        private const val MAX_HD_SIZE_MB = 20
    }

    /**
     * Compresse une image selon la qualité spécifiée
     * @return Résultat contenant le chemin du fichier compressé et les métadonnées
     */
    suspend fun compressImage(
        uri: Uri,
        quality: MediaQuality
    ): Result<CompressionResult> = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return@withContext Result.failure(IOException("Impossible d'ouvrir l'image"))

            // Décoder l'image
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream.close()

            val originalWidth = options.outWidth
            val originalHeight = options.outHeight

            // Calculer la taille cible selon la qualité
            val (maxWidth, maxHeight, jpegQuality) = when (quality) {
                MediaQuality.SD -> Triple(SD_MAX_WIDTH, SD_MAX_HEIGHT, SD_JPEG_QUALITY)
                MediaQuality.HD -> Triple(HD_MAX_WIDTH, HD_MAX_HEIGHT, HD_JPEG_QUALITY)
            }

            // Calculer le ratio de réduction
            val scale = calculateInSampleSize(originalWidth, originalHeight, maxWidth, maxHeight)

            // Décoder l'image avec le ratio de réduction
            val finalOptions = BitmapFactory.Options().apply {
                inSampleSize = scale
            }

            val newInputStream = context.contentResolver.openInputStream(uri)
                ?: return@withContext Result.failure(IOException("Impossible d'ouvrir l'image"))

            var bitmap = BitmapFactory.decodeStream(newInputStream, null, finalOptions)
            newInputStream.close()

            if (bitmap == null) {
                return@withContext Result.failure(IOException("Échec du décodage de l'image"))
            }

            // Corriger l'orientation EXIF
            bitmap = fixOrientation(uri, bitmap)

            // Redimensionner si nécessaire
            if (bitmap.width > maxWidth || bitmap.height > maxHeight) {
                bitmap = resizeBitmap(bitmap, maxWidth, maxHeight)
            }

            // Sauvegarder dans le cache
            val cacheDir = File(context.cacheDir, "media_cache")
            if (!cacheDir.exists()) {
                cacheDir.mkdirs()
            }

            val outputFile = File(cacheDir, "${UUID.randomUUID()}.jpg")
            FileOutputStream(outputFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, jpegQuality, out)
            }

            bitmap.recycle()

            Log.d(TAG, "Image compressed: ${outputFile.length() / 1024}KB (${quality.name})")

            Result.success(
                CompressionResult(
                    filePath = outputFile.absolutePath,
                    size = outputFile.length(),
                    width = bitmap.width,
                    height = bitmap.height,
                    quality = quality
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error compressing image", e)
            Result.failure(e)
        }
    }

    /**
     * Calcule le ratio de réduction optimal
     */
    private fun calculateInSampleSize(
        width: Int,
        height: Int,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    /**
     * Redimensionne un bitmap en conservant le ratio
     */
    private fun resizeBitmap(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        val ratioBitmap = width.toFloat() / height.toFloat()
        val ratioMax = maxWidth.toFloat() / maxHeight.toFloat()

        var finalWidth = maxWidth
        var finalHeight = maxHeight

        if (ratioMax > ratioBitmap) {
            finalWidth = (maxHeight.toFloat() * ratioBitmap).toInt()
        } else {
            finalHeight = (maxWidth.toFloat() / ratioBitmap).toInt()
        }

        return Bitmap.createScaledBitmap(bitmap, finalWidth, finalHeight, true)
    }

    /**
     * Corrige l'orientation de l'image selon les données EXIF
     */
    private fun fixOrientation(uri: Uri, bitmap: Bitmap): Bitmap {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return bitmap
            val exif = ExifInterface(inputStream)
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
            inputStream.close()

            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> rotateBitmap(bitmap, 90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> rotateBitmap(bitmap, 180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> rotateBitmap(bitmap, 270f)
                else -> bitmap
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fixing orientation", e)
            bitmap
        }
    }

    /**
     * Fait pivoter un bitmap
     */
    private fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    /**
     * Copie un fichier vidéo/audio dans le cache
     * Note: La compression vidéo nécessite FFmpeg ou MediaCodec (complexe)
     * Pour l'instant, on copie simplement le fichier
     */
    suspend fun cacheMediaFile(
        uri: Uri,
        type: MediaType,
        quality: MediaQuality
    ): Result<CompressionResult> = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return@withContext Result.failure(IOException("Impossible d'ouvrir le fichier"))

            val cacheDir = File(context.cacheDir, "media_cache")
            if (!cacheDir.exists()) {
                cacheDir.mkdirs()
            }

            val extension = when (type) {
                MediaType.VIDEO -> "mp4"
                MediaType.AUDIO -> "mp3"
                MediaType.IMAGE -> "jpg"
            }

            val outputFile = File(cacheDir, "${UUID.randomUUID()}.$extension")

            outputFile.outputStream().use { output ->
                inputStream.copyTo(output)
            }
            inputStream.close()

            Log.d(TAG, "Media cached: ${outputFile.length() / 1024}KB (${type.name})")

            Result.success(
                CompressionResult(
                    filePath = outputFile.absolutePath,
                    size = outputFile.length(),
                    width = null,
                    height = null,
                    quality = quality
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error caching media file", e)
            Result.failure(e)
        }
    }

    /**
     * Nettoie le cache en supprimant les fichiers au-delà de la limite
     */
    suspend fun cleanCache(maxSizeMB: Long = 500): Int = withContext(Dispatchers.IO) {
        var deletedCount = 0
        try {
            val cacheDir = File(context.cacheDir, "media_cache")
            if (!cacheDir.exists()) return@withContext 0

            val files = cacheDir.listFiles()?.sortedBy { it.lastModified() } ?: return@withContext 0
            var totalSize = files.sumOf { it.length() }
            val maxSizeBytes = maxSizeMB * 1024 * 1024

            // Supprimer les plus vieux fichiers jusqu'à ce qu'on soit sous la limite
            for (file in files) {
                if (totalSize <= maxSizeBytes) break

                totalSize -= file.length()
                if (file.delete()) {
                    deletedCount++
                    Log.d(TAG, "Deleted cache file: ${file.name}")
                }
            }

            Log.d(TAG, "Cache cleaned: $deletedCount files deleted, ${totalSize / 1024 / 1024}MB remaining")
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning cache", e)
        }
        deletedCount
    }

    /**
     * Supprime un fichier du cache
     */
    suspend fun deleteMediaFile(filePath: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = File(filePath)
            if (file.exists()) {
                file.delete()
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting media file", e)
            false
        }
    }
}

/**
 * Résultat de la compression/cache d'un média
 */
data class CompressionResult(
    val filePath: String,
    val size: Long,
    val width: Int?,
    val height: Int?,
    val quality: MediaQuality
)
