# Audit de l'application MemoryShare

**Date** : 20 Fevrier 2026
**Cible** : Application Android MemoryShare (messagerie + reseau social + espaces partages)
**Stack** : Kotlin 2.1.0 / Jetpack Compose / Firebase / Room / Agora SDK

---

## Resume executif

MemoryShare est une application Android mature combinant messagerie instantanee, fil social, stories, appels audio/video et espaces de memoire partages. L'architecture MVVM est bien structuree, mais l'audit revele **plusieurs vulnerabilites de securite critiques**, des problemes de performance lies a la taille des ecrans Compose, et des lacunes dans les regles Firebase qui pourraient etre exploitees.

### Scores par domaine

| Domaine | Note | Commentaire |
|---------|------|-------------|
| Architecture | 7/10 | MVVM correct, mais pas de DI, ViewModelFactory fragile |
| Securite | 4/10 | Failles critiques dans Firestore rules, cleartext traffic, pas de Proguard |
| Qualite du code | 6/10 | Bon pattern general, mais fichiers gigantesques et code duplique |
| Performance | 5/10 | Ecrans de 30k-65k lignes, pas de pagination, listeners non bornes |
| Tests | 2/10 | Quasi-absence de tests unitaires et d'integration |
| Dependencies | 7/10 | Versions recentes, mais Jetifier encore actif |

---

## 1. SECURITE

### 1.1 CRITIQUE - Lecture des messages ouverte a tous les utilisateurs authentifies

**Fichier** : `firestore.rules:67`

```
allow read: if isSignedIn();
```

Tout utilisateur authentifie peut lire **tous les messages** de toutes les conversations. La regle devrait verifier que l'utilisateur est participant de la conversation, comme c'est fait pour la creation (via `isParticipant()`). C'est la faille la plus grave de l'application.

**Correction** :
```
allow read: if isSignedIn() && isParticipantForRead();
```
Avec une fonction qui verifie l'appartenance a la conversation via le champ `conversationId`.

### 1.2 CRITIQUE - Suppression Storage trop permissive

**Fichier** : `storage.rules:55-56, 67, 80, 92, 104, 116`

```
allow delete: if isSignedIn();
```

Tout utilisateur authentifie peut **supprimer n'importe quel fichier** (images de posts, videos, medias de messages, stories, audios). La regle devrait verifier que l'utilisateur est le proprietaire du fichier via les custom metadata.

### 1.3 HAUTE - Cleartext traffic active

**Fichier** : `AndroidManifest.xml:50`

```xml
android:usesCleartextTraffic="true"
```

Autorise les communications HTTP non chiffrees, exposant les donnees a des attaques man-in-the-middle. Doit etre `false` en production.

### 1.4 HAUTE - Proguard/R8 desactive en release

**Fichier** : `app/build.gradle.kts:32`

```kotlin
isMinifyEnabled = false
```

Le code n'est ni obfusque ni minifie en release. Cela facilite le reverse engineering de l'application et augmente la taille de l'APK.

### 1.5 HAUTE - Agora App ID en dur dans le code source

**Fichier** : `AgoraManager.kt:25`

```kotlin
const val APP_ID = "YOUR_AGORA_APP_ID"
```

Bien que c'est un placeholder actuellement, le pattern incite a mettre l'App ID directement dans le code source. Il devrait etre dans `BuildConfig` via `local.properties` ou un systeme de gestion de secrets.

### 1.6 HAUTE - Pas de validation des donnees dans Firestore rules

Les regles Firestore ne valident pas le schema des documents ecrits. Un utilisateur pourrait :
- Creer un post avec des champs arbitraires
- Injecter des `mediaUrls` malveillants
- Modifier le `likeCount` ou `commentCount` directement
- Creer des messages avec un `type` invalide

**Correction** : Ajouter des validations `request.resource.data` pour chaque collection (types, tailles, champs obligatoires).

### 1.7 MOYENNE - Collection "presence" et "typing" sans regles de securite

**Fichier** : `firestore.rules`

Les collections `presence`, `typing`, `fcm_tokens`, et `reels` n'ont **aucune regle definie** dans `firestore.rules`. Par defaut, Firestore refuse l'acces, mais si le mode test est active, ces collections sont ouvertes. Il manque des regles explicites.

### 1.8 MOYENNE - FCM token log en clair

**Fichier** : `MemoryShareMessagingService.kt:80`

```kotlin
Log.d(TAG, "New FCM token: $token")
```

Les tokens FCM sont logges en debug. En production, ces logs pourraient etre accessibles et le token pourrait etre utilise pour envoyer des notifications non autorisees.

### 1.9 MOYENNE - PendingIntent MUTABLE pour DirectReply

**Fichier** : `MemoryShareMessagingService.kt:442`

```kotlin
PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
```

Le `FLAG_MUTABLE` est necessaire pour RemoteInput mais presente un risque si l'Intent peut etre intercepte. Verifier que le BroadcastReceiver est bien `exported="false"` (c'est le cas).

### 1.10 BASSE - Profils utilisateurs lisibles publiquement

**Fichier** : `firestore.rules:18`

```
allow read: if true;
```

Tous les profils, commentaires, stories, medias et follows sont lisibles sans authentification. C'est un choix fonctionnel mais cela expose les emails des utilisateurs via la collection `users`.

### 1.11 BASSE - Pas de re-authentification pour les operations sensibles

**Fichier** : `FirebaseAuthManager.kt:132-153`

Les operations `updatePassword`, `updateEmail`, et `deleteAccount` ne demandent pas de re-authentification prealable. Firebase peut lever une exception `FirebaseAuthRecentLoginRequiredException`, mais elle n'est pas geree specifiquement.

---

## 2. ARCHITECTURE

### 2.1 Pas d'injection de dependances

L'application utilise un pattern singleton via `MemoryShareApplication` avec `lateinit var` pour tous les services. La `ViewModelFactory` prend un parametre `repository: Any` avec des casts non-types.

**Impact** :
- Couplage fort entre les composants
- Difficulte a tester unitairement
- Risques de `ClassCastException` a runtime

**Recommandation** : Adopter Hilt/Dagger ou Koin pour l'injection de dependances.

### 2.2 ViewModelFactory fragile

**Fichier** : `ViewModelFactory.kt:30`

```kotlin
private val repository: Any,
```

Le parametre `repository` est de type `Any`, ce qui perd tout typage a la compilation. Chaque branche effectue un `as` cast non verifie.

### 2.3 Code de mapping duplique

Les fonctions de conversion `DocumentSnapshot.toConversation()`, `DocumentSnapshot.toMessage()` sont dupliquees entre `FirestoreManager.kt` et `RealtimeSyncManager.kt`. Cela cree un risque de desynchronisation si un champ est ajoute.

**Recommandation** : Centraliser les mappings dans un fichier utils ou dans les data classes elles-memes.

### 2.4 Bonne separation des couches

L'architecture respecte bien le pattern MVVM :
- **Data** : Models, DAOs, Repositories
- **Services** : Firebase, Media, Realtime, Calls
- **UI** : Screens, ViewModels, Components, Navigation, Theme

### 2.5 Database avec fallbackToDestructiveMigration

**Fichier** : `AppDatabase.kt:61`

```kotlin
.fallbackToDestructiveMigration()
```

Toute mise a jour du schema detruit les donnees locales. Acceptable en dev mais problematique en production. Des migrations Room devraient etre ecrites.

### 2.6 CoroutineScope non liee au lifecycle

**Fichiers** : `RealtimeSyncManager.kt:41`, `PresenceManager.kt:43`

```kotlin
private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
```

Ces scopes ne sont jamais `cancel()` dans `cleanup()`. Les coroutines pourraient continuer a tourner apres le cleanup.

---

## 3. QUALITE DU CODE

### 3.1 CRITIQUE - Fichiers ecrans demesurement grands

| Fichier | Lignes |
|---------|--------|
| `MessageDetailScreen.kt` | ~66,000 |
| `CreatePostScreen.kt` | ~37,000 |
| `FeedScreen.kt` | ~32,000 |
| `MemorySpaceScreen.kt` | ~26,000 |

Ces fichiers sont extremement difficiles a maintenir, a reviewer et a deboguer. Un fichier de 66,000 lignes est un anti-pattern majeur.

**Recommandation** : Decomposer chaque ecran en sous-composants dans des fichiers separes (header, message list, input bar, dialogs, etc.).

### 3.2 DirectReplyReceiver non fonctionnel

**Fichier** : `MemoryShareMessagingService.kt:490-502`

Le `DirectReplyReceiver` ne fait rien - il log le message mais ne l'envoie jamais. Le commentaire indique que c'est inacheve.

### 3.3 Camera callbacks non implementes

**Fichier** : `AppNavigation.kt:691-698`

```kotlin
onPhotoCaptured = { photoPath ->
    // TODO: Gerer la photo capturee
    navController.popBackStack()
},
```

Les callbacks de capture photo/video de la camera ne sont pas implementes.

### 3.4 FCM token non persiste

**Fichier** : `MemoryShareMessagingService.kt:477-484`

```kotlin
private fun saveTokenToServer(token: String) {
    Log.d(TAG, "FCM Token to save: $token")
    // firestoreManager.saveFcmToken(currentUserId, token)
}
```

Le token FCM n'est jamais envoye au serveur, donc les notifications push ne peuvent pas fonctionner.

### 3.5 Incoherence messages Firestore vs sous-collection

`FirestoreManager` stocke les messages en sous-collection : `conversations/{id}/messages/{id}`
`RealtimeSyncManager.startMessagesSync()` les cherche dans une collection racine : `messages` avec `whereEqualTo("conversationId", ...)`

Cette incoherence signifie que la synchronisation en temps reel ne trouvera pas les messages.

### 3.6 Melange francais/anglais

Le code melange commentaires en francais et en anglais de maniere incoherente. Les notifications alternent entre les deux langues (`"Quelqu'un"` vs `"Someone"`). Choisir une langue unique.

---

## 4. PERFORMANCE

### 4.1 Pas de pagination des donnees

**Fichier** : `FirestoreManager.kt:247`

```kotlin
fun observePosts(limit: Int = 50): Flow<List<Post>>
```

La limite est fixe a 50. Il n'y a pas de pagination par curseur. Pour les messages (`observeMessages`), il n'y a **aucune limite** - tous les messages sont charges d'un coup.

### 4.2 Listeners Firestore potentiellement non bornes

`RealtimeSyncManager` cree un listener pour **chaque conversation** de l'utilisateur sans limite. Un utilisateur avec 100 conversations aurait 101 listeners actifs simultanement, ce qui consomme beaucoup de bande passante et de batterie.

### 4.3 Batch Firestore non borne pour markConversationMessagesRead

**Fichier** : `FirestoreManager.kt:419-441`

La fonction charge **tous** les messages d'une conversation pour les marquer comme lus. Pour une conversation avec des milliers de messages, cela cree un batch enorme. Firestore limite les batches a 500 operations.

### 4.4 Compression d'image sur le thread IO mais en memoire

**Fichier** : `FirebaseStorageManager.kt:357-386`

Les images sont decodees entierement en memoire (`BitmapFactory.decodeStream`) puis recompressees. Pour de tres grandes images, cela peut causer des `OutOfMemoryError`. Le `Compressor` library est dans les dependances mais n'est pas utilise dans `FirebaseStorageManager`.

### 4.5 Jetifier encore actif

**Fichier** : `gradle.properties:5`

```
android.enableJetifier=true
```

Jetifier n'est plus necessaire car toutes les dependances utilisent AndroidX. Il ralentit le build.

---

## 5. TESTS

### 5.1 Quasi-absence de tests

Le projet ne contient que les dependances de test par defaut (JUnit, Espresso, Compose test). Aucun fichier de test n'a ete trouve dans le codebase.

**Impact** : Aucune regression n'est detectee automatiquement. Chaque modification est un risque.

**Recommandation** :
- Tests unitaires pour les ViewModels et Repositories (avec mocks de Firebase)
- Tests d'integration pour les DAOs Room
- Tests UI pour les ecrans critiques (Login, Chat, Feed)
- Objectif minimum : 60% de couverture sur la couche data/domain

---

## 6. DEPENDANCES

### 6.1 Points positifs
- Utilisation de BoM (Bill of Materials) pour Compose et Firebase, assurant la coherence des versions
- Toutes les dependances sont recentes et maintenues
- Compose BOM 2026.01.00 est la derniere version stable

### 6.2 Points d'attention

| Dependance | Observation |
|------------|-------------|
| `enableJetifier=true` | A desactiver - plus necessaire |
| `nonTransitiveRClass=false` | Devrait etre `true` pour de meilleures performances de build |
| `targetSdk = 34` | Devrait etre mis a jour vers 35 ou 36 pour suivre les dernieres exigences Play Store |
| `exportSchema = false` | Empeche la generation du schema Room pour les migrations |
| Agora SDK `4.6.2` | Verifier la conformite avec les politiques de donnees du Play Store |

---

## 7. PLAN D'ACTION PRIORITAIRE

### Immediat (P0 - Bloquant pour production)

1. **Corriger les regles Firestore pour les messages** - Restreindre la lecture aux participants de la conversation
2. **Corriger les regles Storage pour la suppression** - Restreindre au proprietaire du fichier
3. **Desactiver `usesCleartextTraffic`** - Passer a `false`
4. **Activer `isMinifyEnabled`** pour les builds release
5. **Ajouter les regles manquantes** pour `presence`, `typing`, `fcm_tokens`, `reels`

### Court terme (P1 - 2 semaines)

6. **Implementer `saveTokenToServer`** pour que les push notifications fonctionnent
7. **Corriger l'incoherence messages** entre FirestoreManager et RealtimeSyncManager
8. **Ajouter la pagination** pour les messages et les posts
9. **Limiter les listeners** dans RealtimeSyncManager (lazy loading)
10. **Implementer le DirectReplyReceiver**

### Moyen terme (P2 - 1-2 mois)

11. **Decomposer les fichiers ecrans** gigantesques en sous-composants
12. **Adopter Hilt** pour l'injection de dependances
13. **Ecrire des migrations Room** au lieu de `fallbackToDestructiveMigration`
14. **Ajouter des tests** unitaires et d'integration (objectif 60%)
15. **Centraliser le mapping** Firestore -> Model
16. **Valider les donnees** dans les regles Firestore (schema validation)

### Long terme (P3 - Amelioration continue)

17. Mettre en place un pipeline CI/CD avec tests automatiques
18. Ajouter du monitoring (Crashlytics, Performance Monitoring)
19. Implementer un token server pour Agora (ne pas utiliser de token null)
20. Harmoniser la langue (francais ou anglais, pas les deux)
21. Desactiver Jetifier et passer `nonTransitiveRClass` a `true`

---

## Conclusion

L'application MemoryShare a une base solide avec une bonne architecture MVVM et un usage correct des technologies Android modernes (Compose, Room, Coroutines). Cependant, **les failles de securite dans les regles Firestore sont critiques** et doivent etre corrigees avant toute mise en production. La taille demesures des fichiers ecrans et l'absence de tests representent egalement des risques majeurs pour la maintenabilite a long terme.
