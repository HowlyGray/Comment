package com.memoryshare.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class MediaQuality {
    HD,  // Qualité originale (haute définition)
    SD   // Qualité compressée (définition standard)
}

enum class MediaSourceType {
    POST,           // Média d'une publication
    MESSAGE,        // Média d'un message
    SHARED_SPACE,   // Média d'un espace partagé
    STORY,          // Média d'une story
    PROFILE         // Photo de profil ou cover
}

@Entity(tableName = "media_cache")
data class MediaCache(
    @PrimaryKey
    val id: String,                     // ID unique du média
    val localPath: String,              // Chemin du fichier local dans le cache de l'app
    val firebaseUrl: String? = null,    // URL Firebase Storage (null si pas encore uploadé)
    val type: MediaType,                // IMAGE, VIDEO, AUDIO
    val quality: MediaQuality,          // HD ou SD
    val sourceType: MediaSourceType,    // D'où vient ce média
    val sourceId: String,               // ID du post/message/space associé
    val size: Long,                     // Taille en bytes
    val width: Int? = null,             // Largeur pour images/vidéos
    val height: Int? = null,            // Hauteur pour images/vidéos
    val duration: Long? = null,         // Durée en ms pour vidéos/audio
    val isSynced: Boolean = false,      // Uploadé sur Firebase?
    val isDownloaded: Boolean = true,   // Téléchargé localement?
    val createdAt: Long = System.currentTimeMillis(),
    val lastModified: Long = System.currentTimeMillis(),
    val uploadedBy: String              // ID de l'utilisateur qui a uploadé
)
