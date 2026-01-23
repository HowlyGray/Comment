# Configuration des Règles de Sécurité Firebase

## Problème Identifié

Les logs montrent des erreurs `PERMISSION_DENIED: Missing or insufficient permissions` lors des tentatives d'écriture dans Firestore. Cela signifie que Firebase est correctement configuré et connecté, mais les **règles de sécurité Firestore** bloquent les opérations d'écriture.

## Solution : Déployer les Règles de Sécurité

### Option 1 : Via la Console Firebase (Recommandé)

#### Étape 1 : Configurer les Règles Firestore

1. Ouvrez la [Console Firebase](https://console.firebase.google.com/)
2. Sélectionnez votre projet
3. Dans le menu de gauche, cliquez sur **Firestore Database**
4. Allez dans l'onglet **Règles** (Rules)
5. Copiez le contenu du fichier `firestore.rules` et collez-le dans l'éditeur
6. Cliquez sur **Publier** (Publish)

#### Étape 2 : Configurer les Règles Storage

1. Dans la Console Firebase, cliquez sur **Storage**
2. Allez dans l'onglet **Règles** (Rules)
3. Copiez le contenu du fichier `storage.rules` et collez-le dans l'éditeur
4. Cliquez sur **Publier** (Publish)

### Option 2 : Via Firebase CLI

Si vous préférez utiliser la ligne de commande :

```bash
# Installer Firebase CLI (si pas déjà installé)
npm install -g firebase-tools

# Se connecter à Firebase
firebase login

# Initialiser Firebase dans le projet
firebase init

# Sélectionner :
# - Firestore
# - Storage

# Déployer les règles
firebase deploy --only firestore:rules,storage:rules
```

## Règles de Sécurité Implémentées

### Firestore Rules (`firestore.rules`)

Les règles suivantes ont été configurées :

#### 🔓 **Règles Publiques (Lecture)**
- **Users** : Tout le monde peut lire les profils utilisateurs
- **Posts** : Tout le monde peut voir les publications
- **Comments** : Tout le monde peut voir les commentaires
- **Stories** : Tout le monde peut voir les stories
- **User Follows** : Tout le monde peut voir les relations de follow

#### 🔒 **Règles Authentifiées (Écriture)**
- **Users** : Seul l'utilisateur peut créer/modifier son propre profil
- **Posts** : Seul l'auteur peut créer/modifier/supprimer ses posts
- **Comments** : Seul l'auteur peut créer/modifier/supprimer ses commentaires
- **Stories** : Seul l'auteur peut créer/modifier/supprimer ses stories

#### 🔐 **Règles Privées (Lecture + Écriture)**
- **Conversations** : Seuls les participants peuvent accéder
- **Messages** : Seuls les participants de la conversation peuvent lire/écrire
- **Shared Spaces** : Seuls les membres peuvent accéder
- **Calls** : Seuls les participants de l'appel peuvent accéder

### Storage Rules (`storage.rules`)

#### Limites de Taille
- **Images** : Maximum 10 MB
- **Vidéos** : Maximum 100 MB
- **Audio** : Maximum 20 MB

#### Permissions
- **Profile Pictures** : Seul l'utilisateur peut modifier sa photo de profil
- **Post Media** : Tout utilisateur connecté peut uploader, seul l'uploader peut supprimer
- **Message Media** : Uniquement accessible aux utilisateurs connectés
- **Space Media** : Uniquement accessible aux utilisateurs connectés
- **Story Media** : Tout le monde peut lire, seuls les connectés peuvent uploader

## Mode Développement (Temporaire)

⚠️ **Pour le développement uniquement** : Si vous voulez autoriser toutes les opérations sans authentification (NON RECOMMANDÉ EN PRODUCTION) :

### Firestore (Mode Développement)
```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /{document=**} {
      allow read, write: if true;
    }
  }
}
```

### Storage (Mode Développement)
```javascript
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {
    match /{allPaths=**} {
      allow read, write: if true;
    }
  }
}
```

**⚠️ ATTENTION** : Ces règles permettent à n'importe qui de lire et d'écrire dans votre base de données. Utilisez-les UNIQUEMENT pour tester rapidement et remplacez-les par les règles sécurisées dès que possible.

## Activer l'Authentification Firebase

Pour que les règles de sécurité fonctionnent correctement, vous devez activer au moins une méthode d'authentification :

1. Dans la Console Firebase, allez dans **Authentication**
2. Cliquez sur **Get Started**
3. Activez au moins une méthode :
   - **Email/Password** (recommandé pour commencer)
   - **Anonymous** (pour tester sans inscription)
   - **Google Sign-In**
   - Etc.

## Vérification

Après avoir déployé les règles :

1. Redémarrez votre application Android
2. Essayez de créer un post ou d'envoyer un message
3. Vérifiez les logs - les erreurs `PERMISSION_DENIED` devraient disparaître
4. Les données devraient maintenant être synchronisées avec Firebase

## Logs de Succès Attendus

Après la configuration, vous devriez voir dans les logs :

```
FirebaseManager: Document saved: posts/xxx
PostRepository: Post synced to Firebase: xxx
MessageRepository: Message synced to Firebase: xxx
```

Au lieu de :
```
FirebaseManager: Error saving document: PERMISSION_DENIED
```

## Prochaines Étapes

1. ✅ Déployer les règles de sécurité (via Console ou CLI)
2. ✅ Activer l'authentification Firebase (Email/Password ou Anonymous)
3. ✅ Tester la synchronisation des données
4. 🔄 Implémenter l'authentification dans l'app (si pas déjà fait)
5. 🔄 Tester la création de posts, messages, etc.
6. 🔄 Vérifier que les données apparaissent dans la Console Firebase

## Support

Si vous rencontrez des problèmes :
1. Vérifiez que les règles sont bien déployées dans la Console Firebase
2. Vérifiez que l'authentification est activée
3. Vérifiez les logs pour identifier les erreurs spécifiques
4. Consultez la [documentation Firebase](https://firebase.google.com/docs/firestore/security/get-started)
