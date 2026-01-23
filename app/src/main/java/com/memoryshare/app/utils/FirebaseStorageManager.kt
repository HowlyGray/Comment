package com.memoryshare.app.utils

import android.net.Uri
import android.util.Log
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.tasks.await
import java.util.UUID

/**
 * Gestionnaire pour les uploads de médias vers Firebase Storage
 */
class FirebaseStorageManager {
    private val storage = FirebaseStorage.getInstance()
    private val storageRef = storage.reference

    companion object {
        private const val TAG = "FirebaseStorageManager"
    }

    /**
     * Upload une image vers Firebase Storage
     */
    suspend fun uploadImage(uri: Uri, path: String = FirebaseManager.StoragePaths.POST_IMAGES): Result<String> {
        return try {
            val filename = "${UUID.randomUUID()}.jpg"
            val imageRef = storageRef.child("$path/$filename")

            imageRef.putFile(uri).await()
            val downloadUrl = imageRef.downloadUrl.await().toString()

            Log.d(TAG, "Image uploaded successfully: $downloadUrl")
            Result.success(downloadUrl)
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading image", e)
            Result.failure(e)
        }
    }

    /**
     * Upload une vidéo vers Firebase Storage
     */
    suspend fun uploadVideo(uri: Uri, path: String = FirebaseManager.StoragePaths.POST_VIDEOS): Result<String> {
        return try {
            val filename = "${UUID.randomUUID()}.mp4"
            val videoRef = storageRef.child("$path/$filename")

            videoRef.putFile(uri).await()
            val downloadUrl = videoRef.downloadUrl.await().toString()

            Log.d(TAG, "Video uploaded successfully: $downloadUrl")
            Result.success(downloadUrl)
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading video", e)
            Result.failure(e)
        }
    }

    /**
     * Upload un fichier audio vers Firebase Storage
     */
    suspend fun uploadAudio(uri: Uri, path: String = "audios"): Result<String> {
        return try {
            val filename = "${UUID.randomUUID()}.mp3"
            val audioRef = storageRef.child("$path/$filename")

            audioRef.putFile(uri).await()
            val downloadUrl = audioRef.downloadUrl.await().toString()

            Log.d(TAG, "Audio uploaded successfully: $downloadUrl")
            Result.success(downloadUrl)
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading audio", e)
            Result.failure(e)
        }
    }

    /**
     * Upload une image de profil vers Firebase Storage
     */
    suspend fun uploadProfilePicture(uri: Uri, userId: String): Result<String> {
        return try {
            val filename = "$userId.jpg"
            val profileRef = storageRef.child("${FirebaseManager.StoragePaths.PROFILE_PICTURES}/$filename")

            profileRef.putFile(uri).await()
            val downloadUrl = profileRef.downloadUrl.await().toString()

            Log.d(TAG, "Profile picture uploaded successfully: $downloadUrl")
            Result.success(downloadUrl)
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading profile picture", e)
            Result.failure(e)
        }
    }

    /**
     * Supprime un fichier de Firebase Storage
     */
    suspend fun deleteFile(url: String): Result<Unit> {
        return try {
            val fileRef = storage.getReferenceFromUrl(url)
            fileRef.delete().await()
            Log.d(TAG, "File deleted successfully: $url")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting file: $url", e)
            Result.failure(e)
        }
    }

    /**
     * Upload plusieurs fichiers en parallèle
     */
    suspend fun uploadMultipleImages(uris: List<Uri>, path: String = FirebaseManager.StoragePaths.POST_IMAGES): Result<List<String>> {
        return try {
            val urls = mutableListOf<String>()
            for (uri in uris) {
                val result = uploadImage(uri, path)
                result.onSuccess { url ->
                    urls.add(url)
                }.onFailure {
                    throw it
                }
            }
            Log.d(TAG, "Multiple images uploaded successfully: ${urls.size} files")
            Result.success(urls)
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading multiple images", e)
            Result.failure(e)
        }
    }
}
