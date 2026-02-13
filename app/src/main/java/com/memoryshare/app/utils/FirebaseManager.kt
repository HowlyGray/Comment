package com.memoryshare.app.utils

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await

/**
 * Gestionnaire centralisé pour toutes les opérations Firebase
 */
object FirebaseManager {
    private const val TAG = "FirebaseManager"

    // Instances Firebase
    val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    val storage: FirebaseStorage by lazy { FirebaseStorage.getInstance() }
    val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    // Collections Firestore
    object Collections {
        const val USERS = "users"
        const val POSTS = "posts"
        const val COMMENTS = "comments"
        const val MESSAGES = "messages"
        const val CONVERSATIONS = "conversations"
        const val SHARED_SPACES = "shared_spaces"
        const val MEDIA = "media"
        const val STORIES = "stories"
        const val CALLS = "calls"
        const val USER_FOLLOWS = "user_follows"
    }

    // Storage paths
    object StoragePaths {
        const val PROFILE_PICTURES = "profile_pictures"
        const val POST_IMAGES = "post_images"
        const val POST_VIDEOS = "post_videos"
        const val MESSAGE_MEDIA = "message_media"
        const val SPACE_MEDIA = "space_media"
        const val STORY_MEDIA = "story_media"
    }

    /**
     * Sauvegarde un document dans Firestore
     */
    suspend fun <T> saveDocument(
        collection: String,
        documentId: String,
        data: T,
        merge: Boolean = true
    ): Result<Unit> {
        return try {
            if (merge) {
                firestore.collection(collection)
                    .document(documentId)
                    .set(data as Any, SetOptions.merge())
                    .await()
            } else {
                firestore.collection(collection)
                    .document(documentId)
                    .set(data as Any)
                    .await()
            }
            Log.d(TAG, "Document saved: $collection/$documentId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving document: $collection/$documentId", e)
            Result.failure(e)
        }
    }

    /**
     * Récupère un document depuis Firestore
     */
    suspend fun <T> getDocument(
        collection: String,
        documentId: String,
        clazz: Class<T>
    ): Result<T?> {
        return try {
            val document = firestore.collection(collection)
                .document(documentId)
                .get()
                .await()

            val data = document.toObject(clazz)
            Log.d(TAG, "Document retrieved: $collection/$documentId")
            Result.success(data)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting document: $collection/$documentId", e)
            Result.failure(e)
        }
    }

    /**
     * Supprime un document de Firestore
     */
    suspend fun deleteDocument(
        collection: String,
        documentId: String
    ): Result<Unit> {
        return try {
            firestore.collection(collection)
                .document(documentId)
                .delete()
                .await()
            Log.d(TAG, "Document deleted: $collection/$documentId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting document: $collection/$documentId", e)
            Result.failure(e)
        }
    }

    /**
     * Récupère tous les documents d'une collection
     */
    suspend fun <T> getCollection(
        collection: String,
        clazz: Class<T>
    ): Result<List<T>> {
        return try {
            val querySnapshot = firestore.collection(collection)
                .get()
                .await()

            val documents = querySnapshot.documents.mapNotNull { doc ->
                doc.toObject(clazz)
            }
            Log.d(TAG, "Collection retrieved: $collection (${documents.size} items)")
            Result.success(documents)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting collection: $collection", e)
            Result.failure(e)
        }
    }

    /**
     * Observe une collection en temps réel
     */
    fun <T> observeCollection(
        collection: String,
        queryBuilder: (Query) -> Query = { it },
        clazz: Class<T>,
        onUpdate: (List<T>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {
        val query = queryBuilder(firestore.collection(collection))
        return query.addSnapshotListener { snapshot, e ->
            if (e != null) {
                onError(e)
                return@addSnapshotListener
            }
            val items = snapshot?.documents?.mapNotNull { doc ->
                try {
                    doc.toObject(clazz)
                } catch (ex: Exception) {
                    Log.e(TAG, "Error deserializing document: ${doc.id} in $collection", ex)
                    null
                }
            } ?: emptyList()
            onUpdate(items)
        }
    }

    /**
     * Vérifie si l'utilisateur est connecté
     */
    fun isUserSignedIn(): Boolean {
        return auth.currentUser != null
    }

    /**
     * Récupère l'ID de l'utilisateur actuel
     */
    fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }
}
