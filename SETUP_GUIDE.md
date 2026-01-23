# MemoryShare - Guide de Configuration

Ce guide vous explique comment configurer les services backend pour MemoryShare.

## Table des matières

1. [Configuration Firebase](#1-configuration-firebase)
2. [Configuration Agora (Appels)](#2-configuration-agora-appels)
3. [Permissions Android](#3-permissions-android)
4. [Architecture de l'application](#4-architecture-de-lapplication)

---

## 1. Configuration Firebase

### Étape 1: Créer un projet Firebase

1. Allez sur [Firebase Console](https://console.firebase.google.com/)
2. Cliquez sur "Ajouter un projet"
3. Nommez votre projet (ex: "MemoryShare")
4. Activez Google Analytics (optionnel)
5. Cliquez sur "Créer un projet"

### Étape 2: Ajouter l'application Android

1. Dans la console Firebase, cliquez sur l'icône Android
2. **Package name**: `com.memoryshare.app`
3. **App nickname**: MemoryShare (optionnel)
4. **SHA-1**: Obtenez-le avec:
   ```bash
   cd android && ./gradlew signingReport
   ```
5. Cliquez sur "Register app"

### Étape 3: Télécharger google-services.json

1. Téléchargez le fichier `google-services.json`
2. Placez-le dans `app/google-services.json`
3. **IMPORTANT**: Ne commitez jamais ce fichier dans Git!

### Étape 4: Activer les services Firebase

#### Firebase Authentication
1. Dans la console Firebase, allez dans "Authentication"
2. Cliquez sur "Get started"
3. Activez "Email/Password" dans l'onglet "Sign-in method"

#### Cloud Firestore
1. Allez dans "Firestore Database"
2. Cliquez sur "Create database"
3. Choisissez le mode "Production" ou "Test"
4. Sélectionnez une région proche de vos utilisateurs

**Règles de sécurité recommandées:**
```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Users can read/write their own data
    match /users/{userId} {
      allow read: if request.auth != null;
      allow write: if request.auth != null && request.auth.uid == userId;
    }

    // Conversations - participants only
    match /conversations/{conversationId} {
      allow read, write: if request.auth != null &&
        request.auth.uid in resource.data.participantIds;

      match /messages/{messageId} {
        allow read, write: if request.auth != null;
      }
    }

    // Posts - authenticated users
    match /posts/{postId} {
      allow read: if request.auth != null;
      allow write: if request.auth != null &&
        request.auth.uid == resource.data.authorId;
    }

    // Stories - authenticated users
    match /stories/{storyId} {
      allow read: if request.auth != null;
      allow write: if request.auth != null &&
        request.auth.uid == resource.data.authorId;
    }

    // Shared spaces - members only
    match /shared_spaces/{spaceId} {
      allow read: if request.auth != null &&
        request.auth.uid in resource.data.memberIds;
      allow write: if request.auth != null &&
        request.auth.uid == resource.data.creatorId;
    }

    // Presence - own data only
    match /presence/{userId} {
      allow read: if request.auth != null;
      allow write: if request.auth != null && request.auth.uid == userId;
    }

    // FCM Tokens - own data only
    match /fcm_tokens/{userId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
  }
}
```

#### Cloud Storage
1. Allez dans "Storage"
2. Cliquez sur "Get started"
3. Configurez les règles de sécurité

**Règles de sécurité recommandées:**
```javascript
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {
    // Profile pictures
    match /profile_pictures/{userId}.jpg {
      allow read: if request.auth != null;
      allow write: if request.auth != null && request.auth.uid == userId;
    }

    // Chat media
    match /chat_images/{conversationId}/{fileName} {
      allow read, write: if request.auth != null;
    }
    match /chat_videos/{conversationId}/{fileName} {
      allow read, write: if request.auth != null;
    }
    match /chat_audio/{conversationId}/{fileName} {
      allow read, write: if request.auth != null;
    }

    // Post media
    match /post_media/{userId}/{fileName} {
      allow read: if request.auth != null;
      allow write: if request.auth != null && request.auth.uid == userId;
    }

    // Story media
    match /story_media/{userId}/{fileName} {
      allow read: if request.auth != null;
      allow write: if request.auth != null && request.auth.uid == userId;
    }

    // Shared space media
    match /shared_space_media/{spaceId}/{fileName} {
      allow read, write: if request.auth != null;
    }
  }
}
```

#### Firebase Cloud Messaging (FCM)
1. Allez dans "Project settings" > "Cloud Messaging"
2. Notez votre Server key (pour les notifications backend)

---

## 2. Configuration Agora (Appels)

### Étape 1: Créer un compte Agora

1. Allez sur [Agora Console](https://console.agora.io/)
2. Créez un compte gratuit
3. Créez un nouveau projet

### Étape 2: Obtenir l'App ID

1. Dans votre projet Agora, copiez l'**App ID**
2. Ouvrez `app/src/main/java/com/memoryshare/app/services/calls/AgoraManager.kt`
3. Remplacez `YOUR_AGORA_APP_ID` par votre App ID:

```kotlin
companion object {
    const val APP_ID = "votre_app_id_agora_ici"
}
```

### Étape 3: Configuration de la sécurité (Production)

Pour la production, utilisez des tokens temporaires:

1. Activez "App Certificate" dans la console Agora
2. Générez des tokens côté serveur
3. Passez le token lors de l'appel `joinVideoCall(channelName, token)`

**Exemple de génération de token (Node.js):**
```javascript
const { RtcTokenBuilder, RtcRole } = require('agora-access-token');

const appId = 'YOUR_APP_ID';
const appCertificate = 'YOUR_APP_CERTIFICATE';
const channelName = 'test';
const uid = 0;
const role = RtcRole.PUBLISHER;
const expirationTimeInSeconds = 3600;

const token = RtcTokenBuilder.buildTokenWithUid(
  appId, appCertificate, channelName, uid, role, expirationTimeInSeconds
);
```

---

## 3. Permissions Android

L'application demande les permissions suivantes. Assurez-vous de les gérer dans votre UI:

### Permissions requises

| Permission | Usage |
|------------|-------|
| `INTERNET` | Connexion réseau |
| `CAMERA` | Capture photo/vidéo |
| `RECORD_AUDIO` | Messages vocaux, appels |
| `READ_MEDIA_IMAGES` | Accès galerie |
| `READ_MEDIA_VIDEO` | Accès vidéos |
| `READ_MEDIA_AUDIO` | Accès audio |
| `POST_NOTIFICATIONS` | Notifications (Android 13+) |

### Demande de permissions à l'exécution

Utilisez le composant Accompanist Permissions:

```kotlin
@Composable
fun RequestPermissions() {
    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)
    val audioPermission = rememberPermissionState(Manifest.permission.RECORD_AUDIO)

    LaunchedEffect(Unit) {
        cameraPermission.launchPermissionRequest()
        audioPermission.launchPermissionRequest()
    }
}
```

---

## 4. Architecture de l'application

### Structure des packages

```
com.memoryshare.app/
├── data/
│   ├── local/
│   │   ├── dao/          # Data Access Objects (Room)
│   │   └── AppDatabase   # Base de données locale
│   ├── model/            # Modèles de données
│   └── repository/       # Repositories (couche d'abstraction)
│
├── services/
│   ├── firebase/
│   │   ├── FirebaseAuthManager      # Authentification
│   │   ├── FirestoreManager         # Base de données cloud
│   │   └── FirebaseStorageManager   # Stockage fichiers
│   ├── media/
│   │   ├── CameraManager            # CameraX
│   │   └── AudioRecorderManager     # Enregistrement audio
│   ├── realtime/
│   │   ├── PresenceManager          # Statut en ligne
│   │   └── RealtimeSyncManager      # Synchronisation temps réel
│   ├── calls/
│   │   └── AgoraManager             # Appels audio/vidéo
│   ├── MemoryShareMessagingService  # Notifications FCM
│   └── CallService                  # Service d'appel foreground
│
├── ui/
│   ├── screens/          # Écrans Compose
│   ├── components/       # Composants réutilisables
│   ├── viewmodel/        # ViewModels
│   └── navigation/       # Navigation
│
└── MemoryShareApplication.kt  # Application principale
```

### Flux de données

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│   UI (Compose)  │ ──> │   ViewModel     │ ──> │   Repository    │
│                 │ <── │   (StateFlow)   │ <── │                 │
└─────────────────┘     └─────────────────┘     └────────┬────────┘
                                                         │
                    ┌────────────────────────────────────┼────────────────────────────────────┐
                    │                                    │                                    │
                    ▼                                    ▼                                    ▼
          ┌─────────────────┐               ┌─────────────────┐               ┌─────────────────┐
          │   Room (Local)  │               │    Firestore    │               │  Firebase Auth  │
          │                 │               │    (Cloud)      │               │                 │
          └─────────────────┘               └─────────────────┘               └─────────────────┘
```

### Services disponibles

| Service | Description |
|---------|-------------|
| `FirebaseAuthManager` | Gestion de l'authentification utilisateur |
| `FirestoreManager` | CRUD et écoute temps réel Firestore |
| `FirebaseStorageManager` | Upload/download de fichiers |
| `CameraManager` | Capture photo et vidéo avec CameraX |
| `AudioRecorderManager` | Enregistrement de messages vocaux |
| `PresenceManager` | Gestion du statut en ligne/hors ligne |
| `RealtimeSyncManager` | Synchronisation Firestore ↔ Room |
| `AgoraManager` | Appels audio/vidéo avec Agora SDK |

---

## Prochaines étapes

1. ✅ Configurer Firebase (Auth, Firestore, Storage)
2. ✅ Configurer Agora pour les appels
3. ⬜ Implémenter un backend pour les notifications push
4. ⬜ Ajouter la gestion des tokens Agora côté serveur
5. ⬜ Configurer CI/CD pour les builds automatiques
6. ⬜ Ajouter des tests unitaires et d'intégration

---

## Support

Pour toute question ou problème:
- Consultez la [documentation Firebase](https://firebase.google.com/docs)
- Consultez la [documentation Agora](https://docs.agora.io/)
- Ouvrez une issue sur le repository GitHub
