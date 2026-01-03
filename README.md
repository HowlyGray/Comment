# MemoryShare

Une application Android de messagerie et de partage de souvenirs audio et vidéo, combinant les fonctionnalités de WhatsApp, Instagram et Google Photos.

## Fonctionnalités

### 📱 Messagerie (Comme WhatsApp)
- Conversations individuelles et de groupe
- Messages texte, images, vidéos et audio
- Historique des conversations
- Notifications de messages non lus
- Interface de chat en temps réel

### 📸 Feed d'actualité (Comme Instagram)
- Publication de photos et vidéos
- Légendes et descriptions
- Système de likes
- Commentaires sur les publications
- Fil d'actualité personnalisé

### 🎥 Espaces Partagés (Comme Google Photos)
- Création d'espaces de souvenirs partagés
- Partage de photos, vidéos et audio
- Contrôle d'accès par espace (seuls les membres invités peuvent voir le contenu)
- Organisation par espaces thématiques
- Filtrage par type de média (photos, vidéos, audio)
- Gestion des membres

## Architecture

L'application suit l'architecture MVVM (Model-View-ViewModel) recommandée par Google :

```
app/
├── data/
│   ├── local/
│   │   ├── dao/           # Data Access Objects (Room)
│   │   ├── AppDatabase.kt # Base de données Room
│   │   └── Converters.kt  # Convertisseurs de types
│   ├── model/             # Modèles de données
│   └── repository/        # Repositories (couche d'abstraction)
├── ui/
│   ├── components/        # Composants réutilisables
│   ├── navigation/        # Navigation de l'app
│   ├── screens/           # Écrans de l'application
│   ├── theme/             # Thème Material Design 3
│   └── viewmodel/         # ViewModels
└── MainActivity.kt
```

## Technologies Utilisées

- **Langage**: Kotlin 100%
- **UI**: Jetpack Compose (Material Design 3)
- **Architecture**: MVVM
- **Base de données**: Room
- **Navigation**: Navigation Compose
- **Gestion d'état**: StateFlow
- **Coroutines**: Pour les opérations asynchrones
- **Chargement d'images**: Coil
- **Lecture média**: ExoPlayer (Media3)

## Modèles de Données

### User
- Informations utilisateur (nom, email, photo de profil, bio)

### Conversation
- Gestion des conversations (individuelles et groupes)
- Derniers messages et timestamps

### Message
- Messages avec support multimédia (texte, image, vidéo, audio)
- Statut de lecture

### Post
- Publications avec médias
- Likes et commentaires
- Légendes

### SharedSpace
- Espaces partagés de souvenirs
- Liste des membres avec accès
- Statistiques (nombre de médias, dernière activité)

### Media
- Fichiers médias (photos, vidéos, audio)
- Métadonnées (titre, description, durée, dimensions)

## Écrans Principaux

1. **LoginScreen**: Authentification utilisateur
2. **MessagesScreen**: Liste des conversations
3. **MessageDetailScreen**: Vue détaillée d'une conversation
4. **FeedScreen**: Fil d'actualité avec publications
5. **PostDetailScreen**: Détail d'une publication avec commentaires
6. **MemoriesScreen**: Liste des espaces partagés
7. **MemorySpaceScreen**: Contenu d'un espace partagé
8. **ProfileScreen**: Profil utilisateur

## Installation

### Prérequis
- Android Studio Hedgehog ou plus récent
- JDK 17
- Android SDK 34
- Minimum SDK: 26 (Android 8.0)

### Étapes

1. Clonez le repository:
```bash
git clone https://github.com/votre-repo/memoryshare.git
cd memoryshare
```

2. Ouvrez le projet dans Android Studio

3. Synchronisez les dépendances Gradle

4. Lancez l'application sur un émulateur ou appareil physique

## Permissions Requises

L'application nécessite les permissions suivantes :

- `INTERNET` - Pour les fonctionnalités réseau
- `READ_EXTERNAL_STORAGE` / `READ_MEDIA_IMAGES` / `READ_MEDIA_VIDEO` / `READ_MEDIA_AUDIO` - Pour accéder aux médias
- `CAMERA` - Pour capturer des photos/vidéos
- `RECORD_AUDIO` - Pour enregistrer de l'audio

## Fonctionnalités à Venir

- [ ] Intégration avec un backend (Firebase ou API personnalisée)
- [ ] Synchronisation cloud des données
- [ ] Notifications push
- [ ] Appels audio/vidéo
- [ ] Stories temporaires (24h)
- [ ] Chiffrement de bout en bout pour les messages
- [ ] Téléchargement de médias en haute résolution
- [ ] Albums photo automatiques
- [ ] Reconnaissance faciale pour taguer les personnes
- [ ] Géolocalisation des souvenirs

## Structure de la Base de Données

La base de données Room contient les tables suivantes :

- `users` - Utilisateurs
- `conversations` - Conversations
- `messages` - Messages
- `posts` - Publications
- `comments` - Commentaires
- `shared_spaces` - Espaces partagés
- `media` - Fichiers médias

## Contribution

Les contributions sont les bienvenues ! N'hésitez pas à :

1. Fork le projet
2. Créer une branche pour votre fonctionnalité (`git checkout -b feature/AmazingFeature`)
3. Commit vos changements (`git commit -m 'Add some AmazingFeature'`)
4. Push vers la branche (`git push origin feature/AmazingFeature`)
5. Ouvrir une Pull Request

## License

Ce projet est sous licence MIT.

## Auteur

Développé avec ❤️ en Kotlin

---

**Note**: Cette application est actuellement en mode hors-ligne avec stockage local. L'intégration backend sera ajoutée dans une future version.
