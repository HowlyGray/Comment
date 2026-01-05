# Configuration Firebase et Services Externes

Ce document explique comment configurer Firebase et les services externes pour activer toutes les fonctionnalités de MemoryShare.

## 📱 Firebase Cloud Messaging (Notifications Push)

### Étape 1 : Créer un projet Firebase
1. Allez sur https://console.firebase.google.com/
2. Cliquez sur "Ajouter un projet"
3. Suivez les étapes de configuration

### Étape 2 : Ajouter votre app Android
1. Dans la console Firebase, cliquez sur l'icône Android
2. Entrez le package name : `com.memoryshare.app`
3. Téléchargez le fichier `google-services.json`
4. Placez-le dans `app/` (pas `app/src/`)

### Étape 3 : Ajouter les dépendances
Dans `build.gradle.kts` (niveau projet), ajoutez :
```kotlin
buildscript {
    dependencies {
        classpath("com.google.gms:google-services:4.4.0")
    }
}
```

Dans `app/build.gradle.kts`, ajoutez :
```kotlin
plugins {
    id("com.google.gms.google-services")
}

dependencies {
    // Firebase BOM
    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))

    // Firebase Cloud Messaging
    implementation("com.google.firebase:firebase-messaging-ktx")

    // Firebase Storage
    implementation("com.google.firebase:firebase-storage-ktx")

    // Firebase Analytics (optionnel)
    implementation("com.google.firebase:firebase-analytics-ktx")
}
```

### Étape 4 : Créer le service de notifications
Créez `FirebaseMessagingService.kt` :
```kotlin
package com.memoryshare.app.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.memoryshare.app.MainActivity
import com.memoryshare.app.R

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // Gérer les données reçues
        remoteMessage.notification?.let {
            sendNotification(it.title, it.body)
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Envoyer le token au serveur
        // TODO: Sauvegarder dans SharedPreferences et synchroniser avec le backend
    }

    private fun sendNotification(title: String?, messageBody: String?) {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = "memoryshare_notifications"
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Créer le canal de notification pour Android O et supérieur
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Notifications MemoryShare",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(0, notificationBuilder.build())
    }
}
```

### Étape 5 : Ajouter le service dans AndroidManifest.xml
```xml
<service
    android:name=".services.MyFirebaseMessagingService"
    android:exported="false">
    <intent-filter>
        <action android:name="com.google.firebase.MESSAGING_EVENT" />
    </intent-filter>
</service>
```

---

## 📦 Firebase Storage (Stockage de Médias)

### Configuration dans le code

Créez `FirebaseStorageManager.kt` :
```kotlin
package com.memoryshare.app.utils

import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.tasks.await
import java.util.UUID

class FirebaseStorageManager {
    private val storage = FirebaseStorage.getInstance()
    private val storageRef = storage.reference

    suspend fun uploadImage(uri: Uri, path: String = "images"): String {
        val filename = "${UUID.randomUUID()}.jpg"
        val imageRef = storageRef.child("$path/$filename")

        imageRef.putFile(uri).await()
        return imageRef.downloadUrl.await().toString()
    }

    suspend fun uploadVideo(uri: Uri, path: String = "videos"): String {
        val filename = "${UUID.randomUUID()}.mp4"
        val videoRef = storageRef.child("$path/$filename")

        videoRef.putFile(uri).await()
        return videoRef.downloadUrl.await().toString()
    }

    suspend fun uploadAudio(uri: Uri, path: String = "audios"): String {
        val filename = "${UUID.randomUUID()}.mp3"
        val audioRef = storageRef.child("$path/$filename")

        audioRef.putFile(uri).await()
        return audioRef.downloadUrl.await().toString()
    }

    suspend fun deleteFile(url: String) {
        try {
            val fileRef = storage.getReferenceFromUrl(url)
            fileRef.delete().await()
        } catch (e: Exception) {
            // Gérer l'erreur
        }
    }
}
```

### Configuration des règles de sécurité
Dans la console Firebase > Storage > Rules :
```
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {
    match /{allPaths=**} {
      allow read: if true;
      allow write: if request.auth != null;
    }
  }
}
```

---

## 📞 Appels Audio/Vidéo (WebRTC)

Pour les appels audio/vidéo, deux options :

### Option 1 : Agora (Recommandé)
1. Créer un compte sur https://www.agora.io/
2. Obtenir l'APP ID
3. Ajouter la dépendance :
```kotlin
implementation("io.agora.rtc:full-sdk:4.2.6")
```

4. Documentation : https://docs.agora.io/en/video-calling/get-started/get-started-sdk

### Option 2 : Twilio
1. Créer un compte sur https://www.twilio.com/
2. Obtenir l'API Key et Secret
3. Ajouter la dépendance :
```kotlin
implementation("com.twilio:video-android:7.6.0")
```

4. Documentation : https://www.twilio.com/docs/video

---

## 🔄 Backend Sync API (Optionnel)

Pour synchroniser les données entre appareils, vous pouvez :

### Option 1 : Firebase Firestore
```kotlin
implementation("com.google.firebase:firebase-firestore-ktx")
```

### Option 2 : Backend personnalisé
Créer un API REST avec :
- Node.js + Express + MongoDB
- Spring Boot + PostgreSQL
- Laravel + MySQL

Points d'accès recommandés :
- `POST /api/users/register`
- `POST /api/users/login`
- `GET /api/posts`
- `POST /api/posts`
- `GET /api/messages`
- `POST /api/messages`
- Etc.

---

## ✅ Vérification de la configuration

Pour vérifier que Firebase est bien configuré :

```kotlin
// Dans MainActivity onCreate()
FirebaseApp.initializeApp(this)
Log.d("Firebase", "Initialized successfully")

// Obtenir le token FCM
FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
    if (task.isSuccessful) {
        val token = task.result
        Log.d("FCM Token", token)
    }
}
```

---

## 📝 Notes importantes

1. **Permissions** : Ajoutez dans AndroidManifest.xml :
```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />
<uses-permission android:name="android.permission.READ_MEDIA_VIDEO" />
<uses-permission android:name="android.permission.READ_MEDIA_AUDIO" />
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.RECORD_AUDIO" />
```

2. **ProGuard** : Si vous activez ProGuard, ajoutez :
```
-keep class com.google.firebase.** { *; }
-keep class io.agora.** { *; }
```

3. **Sécurité** : Ne commitez JAMAIS :
   - `google-services.json`
   - Les clés API dans le code
   - Utilisez `local.properties` ou `BuildConfig`

---

## 🚀 Prochaines étapes

Après configuration :
1. Tester les notifications push
2. Tester l'upload de médias
3. Implémenter les appels si nécessaire
4. Configurer le backend si nécessaire

Pour toute question, consultez la documentation officielle Firebase : https://firebase.google.com/docs
