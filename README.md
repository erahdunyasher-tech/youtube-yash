# YouTube Libre — application Android gratuite

Application Android native (Kotlin, Material Design 3) permettant d'explorer YouTube :
recherche de vidéos et de chaînes via la **YouTube Data API v3**, lecture dans le
**lecteur officiel intégré par YouTube**, favoris et historique stockés localement,
mode sombre et Picture-in-Picture.

> **Aucune publicité ajoutée, aucun abonnement, aucune extraction de flux vidéo,
> aucun contournement des règles de YouTube.** Les publicités éventuellement
> diffusées sont celles de YouTube, dans le lecteur officiel.

[![Build Android](https://github.com/erahdunyasher-tech/youtube-yash/actions/workflows/android.yml/badge.svg)](https://github.com/erahdunyasher-tech/youtube-yash/actions/workflows/android.yml)
[![Deploy documentation](https://github.com/erahdunyasher-tech/youtube-yash/actions/workflows/web.yml/badge.svg)](https://github.com/erahdunyasher-tech/youtube-yash/actions/workflows/web.yml)
[![Licence MIT](https://img.shields.io/badge/Licence-MIT-16a34a.svg)](LICENSE)
[![Android 8.0+](https://img.shields.io/badge/Android-8.0%2B-3DDC84.svg)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.24-7F52FF.svg)](https://kotlinlang.org)
[![Material 3](https://img.shields.io/badge/UI-Material%203-757575.svg)](https://m3.material.io)

---

## 1. Sommaire

1. [Fonctionnalités](#2-fonctionnalités)
2. [Architecture du projet](#3-architecture-du-projet)
3. [Configuration de l'API YouTube](#4-configuration-de-lapi-youtube)
4. [Compilation et génération de l'APK](#5-compilation-et-génération-de-lapk)
5. [Publier sur GitHub](#6-publier-sur-github)
6. [Tests](#7-tests)
7. [Limites imposées par YouTube](#8-limites-imposées-par-youtube)
8. [Confidentialité et données collectées](#9-confidentialité-et-données-collectées)
9. [Dépannage](#10-dépannage)

---

## 2. Fonctionnalités

| Domaine | Détail |
|---|---|
| Interface | Material Design 3, thème clair/sombre/système, onglets de navigation, miniatures optimisées (Coil) |
| Recherche | Recherche de vidéos **et** de chaînes, filtres (Tout / Vidéos / Chaînes), debounce 400 ms, pagination, recherches récentes |
| Tendances | Vidéos populaires par catégorie (Musique, Jeux, Films, Actualités, Sports, Apprentissage) |
| Lecture | Lecteur officiel YouTube embarqué (WebView `/embed`), commandes play/pause, partage, ouverture dans l'application YouTube |
| Picture-in-Picture | Bouton dédié + bascule automatique à la sortie de l'application si la vidéo est en lecture (Android 8+ ; support OEM variable) |
| Favoris | Ajout/retrait depuis l'écran de lecture, persistance locale (Room) |
| Historique | Enregistrement automatique des vidéos regardées, taille configurable, purge manuelle |
| Robustesse | Cache mémoire TTL (5 min) + cache HTTP disque (10 Mo) pour économiser le quota, messages d'erreur contextualisés (quota, clé invalide, réseau, 429) |
| Confidentialité | Aucune télémétrie, aucun compte, aucune donnée transmise à un tiers en dehors de l'API YouTube |

### Arborescence (principaux fichiers)

```
android/
├── settings.gradle.kts / build.gradle.kts / gradle.properties
├── gradle/wrapper/gradle-wrapper.properties          # Gradle 8.7
└── app/
    ├── build.gradle.kts                              # AGP 8.5.2, Kotlin 1.9.24, KSP, Room, Retrofit, Coil
    └── src/
        ├── main/
        │   ├── AndroidManifest.xml                    # PiP, deep link ytlibre://video/<id>
        │   ├── java/com/ytlibre/app/
        │   │   ├── YouTubeLibreApp.kt                 # Application + thème
        │   │   ├── di/AppContainer.kt                 # Injection de dépendances manuelle
        │   │   ├── data/
        │   │   │   ├── local/                         # Room (AppDatabase), SecurePreferences (keystore)
        │   │   │   ├── remote/                        # Retrofit, DTO, mappers, ApiErrorMapper, NetworkModule
        │   │   │   ├── model/                         # Modèles de domaine
        │   │   │   └── repository/                    # YouTubeRepository, LibraryRepository
        │   │   └── ui/
        │   │       ├── common/                        # Adapters, UiState, AppError, AppResult
        │   │       ├── main/                          # MainActivity (navigation + PiP)
        │   │       ├── home/ search/ watch/ channel/ library/ settings/
        │   │       └── watch/PlayerWebView.kt         # Lecteur officiel + pont JavaScript
        │   └── res/                                   # Layouts XML, menu, navigation, thèmes, icônes
        ├── test/                                      # Tests unitaires JVM
        └── androidTest/                               # Tests instrumentés (Room, Espresso)
server/proxy.mjs                                      # Proxy de référence : clé API côté serveur
```

---

## 3. Architecture du projet

Architecture en couches, sans dépendance inutile, testable et remplaçable :

```
┌──────────────────────────────────────────────────────────┐
│ UI (Fragments XML + ViewBinding)                         │
│ Home · Search · Watch · Channel · Library · Settings     │
└───────────────▲──────────────────────────────────────────┘
                │ StateFlow / Flow
┌───────────────┴──────────────────────────────────────────┐
│ ViewModels (androidx.lifecycle)                         │
│ UiState<Success | Error> · actions · debounce            │
└───────────────▲──────────────────────────────────────────┘
                │
┌───────────────┴──────────────────────────────────────────┐
│ Repositories                                            │
│ YouTubeRepository (API + cache TTL) · LibraryRepository  │
└───────▲───────────────────────────────▲──────────────────┘
        │                               │
┌───────┴─────────┐           ┌─────────┴──────────────────┐
│ Data API v3     │           │ Local (Room + Keystore)    │
│ Retrofit/OkHttp │           │ favoris · historique · clé  │
└─────────────────┘           └────────────────────────────┘
```

* **UI** : fragments XML + ViewBinding, `StateFlow` collecté via `repeatOnLifecycle`.
* **ViewModels** : aucun accès direct au réseau, états `Idle / Loading / Success / Error`.
* **Repositories** : `YouTubeRepository` (mémorisation + quota), `LibraryRepository` (données locales).
* **Data** : Retrofit/OkHttp (cache HTTP, intercepteurs User-Agent et Cache-Control), Room, `EncryptedSharedPreferences`.

### Choix techniques notables

* **Lecteur officiel** : `com.google.android.youtube:youtube-android-player-api` est déprécié par Google.
  L'application utilise donc la voie officiellement recommandée : la page `/embed` de YouTube
  servie dans une `WebView`, pilotée par l'IFrame Player API (commandes `playVideo`/`pauseVideo`
  et événements `onStateChange`).
* **Sécurité de la clé API** : deux modes.
  * *Direct* : la clé est saisie par l'utilisateur et stockée **chiffrée** dans
    `EncryptedSharedPreferences` (keystore Android, AES-256-GCM).
  * *Proxy* : la clé vit **uniquement** dans l'environnement du serveur
    (`YOUTUBE_API_KEY`); l'appareil n'envoie jamais de paramètre `key`.
* **Injection de dépendances** : conteneur manuel (`AppContainer`) : pas de framework
  supplémentaire, démarrage rapide, remplacement trivial par Hilt/Koin.

---

## 4. Configuration de l'API YouTube

### 4.1 Créer un projet Google Cloud et activer l'API

1. Ouvrir la [console Google Cloud](https://console.cloud.google.com/).
2. Créer un projet (ou en sélectionner un existant).
3. Menu **APIs et services → Bibliothèque** → rechercher **YouTube Data API v3** → **Activer**.
4. Menu **APIs et services → Identifiants** → **Créer des identifiants → Clé API**.
5. Restreindre la clé :
   * *Restrictions d'applications* : **Applications Android** → nom du package `com.ytlibre.app`
     + empreinte SHA-1 du certificat de debug (ou de release) ;
   * *Restrictions d'API* : **YouTube Data API v3**.
6. Copier la clé (elle commence par `AIza…`).

> Le quota gratuit par défaut est de **10 000 unités/jour** (la recherche coûte 100 unités
> par requête, `videos.list` et `channels.list` 1 unité). L'application limite les appels
> grâce au cache mémoire et au cache HTTP.

### 4.2 Mode 1 — clé API sur l'appareil (usage personnel)

Dans l'application : **Réglages → Clé API YouTube Data API v3 → coller la clé → Enregistrer**.
La clé est chiffrée au repos (keystore Android) et n'est jamais écrite dans les logs.

### 4.3 Mode 2 — proxy serveur (recommandé pour une distribution)

```bash
# Développement local
YOUTUBE_API_KEY=AIza... PORT=8080 node server/proxy.mjs

# Production (derrière un reverse proxy HTTPS : nginx, Caddy, Cloudflare Tunnel…)
YOUTUBE_API_KEY=AIza... PORT=8080 node server/proxy.mjs
```

Dans l'application : **Réglages → Proxy serveur → `https://votre-domaine.example/youtube/v3/`**.

Le proxy :
* n'expose que `search`, `videos` et `channels` (lecture seule) ;
* **ignore** toute clé envoyée par le client et utilise celle du serveur ;
* met en cache les réponses 60 s pour réduire la consommation de quota ;
* ne journalise ni les URLs contenant `key=` ni les données utilisateur.

### 4.4 Vérification rapide de la configuration

```bash
curl "https://www.googleapis.com/youtube/v3/search?part=snippet&q=kotlin&type=video&maxResults=1&key=AIza..."
```

Erreurs courantes :

| Erreur | Cause | Correction |
|---|---|---|
| `403 keyInvalid` | clé incorrecte | vérifier la clé et l'activation de l'API |
| `403 accessNotConfigured` | API non activée | activer « YouTube Data API v3 » dans le projet |
| `403 quotaExceeded` | quota épuisé | attendre le reset, utiliser le proxy, réduire la fréquence |
| `400 badRequest` | paramètres invalides | vérifier `type`, `q`, `maxResults` |

---

## 5. Compilation et génération de l'APK

### 5.1 Prérequis

| Outil | Version |
|---|---|
| JDK | 17 (JBR fourni avec Android Studio) |
| Android Studio | Koala (2024.1) ou supérieur |
| Android SDK | API 34 (compile), API 26 minimum |
| Gradle | 8.7 (fourni par le wrapper) |

### 5.2 Avec Android Studio

1. **File → Open** → sélectionner le dossier `android/`.
2. Laisser la synchronisation Gradle se terminer (le wrapper télécharge Gradle 8.7).
3. **Build → Build Bundle(s) / APK(s) → Build APK(s)**.
4. L'APK est généré dans `android/app/build/outputs/apk/debug/app-debug.apk`.
5. Installer sur un appareil : `adb install -r app/build/outputs/apk/debug/app-debug.apk`.

### 5.3 En ligne de commande

> Le binaire `gradle/wrapper/gradle-wrapper.jar` et le script `gradlew` ne sont pas
> versionnés ici. Ouvrez une première fois le projet dans Android Studio (qui les
> régénère), ou lancez `gradle wrapper --gradle-version 8.7` avec un Gradle local.

```bash
cd android

# Vérification complète (compilation + tests unitaires + lint)
./gradlew clean build

# Tests unitaires JVM (Formatters, ApiErrorMapper, YouTubeRepository)
./gradlew test

# Tests instrumentés (nécessite un appareil ou un émulateur actif)
./gradlew connectedAndroidTest

# Rapport de couverture
./gradlew test jacocoTestReport

# APK debug
./gradlew assembleDebug
# -> app/build/outputs/apk/debug/app-debug.apk

# APK release (signature de debug : à remplacer en production)
./gradlew assembleRelease
# -> app/build/outputs/apk/release/app-release.apk

# Bundle Play Store (AAB)
./gradlew bundleRelease
```

### 5.4 Signature pour la production

```bash
keytool -genkey -v -keystore ytl-release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias ytl

# android/keystore.properties (non versionné)
storeFile=../ytl-release.jks
storePassword=…
keyAlias=ytl
keyPassword=…

./gradlew bundleRelease
```

---

## 6. Publier sur GitHub

Le dépôt est prêt à être publié : `.gitignore` protège les secrets
(`keystore.properties`, `*.jks`, `local.properties`), `LICENSE` (MIT),
`SECURITY.md`, `CONTRIBUTING.md`, modèle d'issue et **3 workflows GitHub Actions** :

| Workflow | Rôle |
|---|---|
| `.github/workflows/android.yml` | Tests unitaires + lint + APK debug (artifact) à chaque push |
| `.github/workflows/release.yml` | APK **signé** publié dans une GitHub Release lors d'un tag `v*` |
| `.github/workflows/web.yml` | Déploiement du site de documentation sur GitHub Pages |

### Mise en ligne (5 minutes)

Le script `publish-github.sh` fait tout : identité Git, commit, création du
dépôt GitHub (via `gh` ou l’API) et push. Le jeton est lu au terminal et
n’est **jamais** écrit dans le dépôt.

```bash
./publish-github.sh youtube-yash public
```

Équivalent manuel :

```bash
# 1. Créer le dépôt (vide) sur github.com, puis :
git init -b main
git add .
git commit -m "feat: YouTube Libre v1.0.0 — app Android Kotlin (Material 3) + docs"
git remote add origin https://github.com/erahdunyasher-tech/youtube-yash.git
git push -u origin main
```

### Publier un APK signé

```bash
# Générer la clé (une seule fois, à garder en lieu sûr)
keytool -genkey -v -keystore ytl-release.jks -keyalg RSA -keysize 2048 \
        -validity 10000 -alias ytl

# Secrets du dépôt (Settings > Secrets and variables > Actions)
KEYSTORE_BASE64  = base64 -w0 ytl-release.jks
KEYSTORE_PASSWORD = <mot de passe du magasin>
KEY_ALIAS         = ytl
KEY_PASSWORD      = <mot de passe de la clé>

# Déclencher la release
git tag v1.0.0 && git push origin v1.0.0
```

L’APK signé est alors téléchargeable depuis la page *Releases* du dépôt.
Pour publier le site de documentation : **Settings → Pages → Source : GitHub Actions**.

---

## 7. Tests

### 6.1 Tests unitaires (JVM) — `app/src/test/`

| Classe | Couverture |
|---|---|
| `FormattersTest` | parsing des durées ISO-8601, formatage des durées, vues, dates relatives, normalisation des requêtes |
| `ApiErrorMapperTest` | quota épuisé, limite quotidienne, clé invalide, 429 + `Retry-After`, erreur 5xx, erreurs réseau |
| `YouTubeRepositoryTest` | requête envoyée (paramètres `key`, `type`, `q`), mapping des DTO, pagination, erreur quota, cache mémoire (2ᵉ appel sans requête réseau) |

Exécution : `./gradlew test` — rapport dans `android/app/build/reports/tests/testDebugUnitTest/index.html`.

### 6.2 Tests instrumentés — `app/src/androidTest/`

| Classe | Couverture |
|---|---|
| `LibraryDaoTest` | insertion/suppression d'un favori, tri des favoris, purge de l'historique au-delà de la limite |
| `MainActivitySmokeTest` | démarrage de l'application et affichage de la navigation (sans clé API) |

Exécution : `./gradlew connectedAndroidTest` (émulateur ou appareil requis).

### 6.3 Scénarios manuels à vérifier

1. **Recherche** : saisir un mot-clé → résultats (vidéos + chaînes) → changer de filtre → pagination.
2. **Lecture** : ouvrir une vidéo → lecture dans le lecteur officiel → pause/reprise → PiP → retour.
3. **Favoris** : ajouter/retirer un favori → vérifier sa persistance après redémarrage.
4. **Historique** : regarder 3 vidéos → vérifier l'ordre (plus récent en premier) → vider l'historique.
5. **Mode sombre** : Réglages → Thème → Sombre → l'interface bascule (Material 3, `values-night`).
6. **Quota** : avec une clé invalide → message d'erreur explicite ; avec un quota épuisé → message dédié.
7. **Hors ligne** : mode avion → message « Connexion impossible », pas de crash.

---

## 8. Limites imposées par YouTube

Ces limites sont **imposées par YouTube** et ne sont pas des choix de conception. L'application ne les contourne pas.

1. **Publicité de YouTube** : le lecteur officiel peut diffuser ses propres publicités, notamment avant, pendant ou après une vidéo. L'application **n'ajoute aucune publicité** et ne peut pas — ni ne doit — supprimer celles de YouTube.
2. **Lecture en arrière-plan** : la lecture hors de l'application dépend du lecteur officiel et des OEM. Elle **n'est pas garantie** ; l'application ne force ni service de fond ni extraction de flux.
3. **Disponibilité régionale** : certaines vidéos sont bloquées par YouTube dans certains pays. L'embed affiche alors « Vidéo non disponible ».
4. **Vidéos.age-restricted / Membres / Privées** : l'embed ne peut pas afficher les vidéos privées, réservées aux membres ou à restriction d'âge sans authentification.
5. **Embeddings désactivés** : le propriétaire d'une vidéo peut **interdire la lecture intégrée** : l'embed affiche alors « Vidéo non disponible » ou « Lecture sur YouTube uniquement ».
6. **Quota d'API** : 10 000 unités/jour par défaut. La recherche coûte 100 unités. L'application limite les appels (cache), mais ne peut pas supprimer la limite.
7. **Clé API et attribution** : une clé Data API utilisée par une application tiers doit respecter les conditions d'utilisation de Google (restrictions d'application, identification de l'application). L'utilisateur fournit sa propre clé ou configure un proxy.
8. **Conditions d'utilisation de l'API YouTube** : pas de stockage massif, pas de redistribution, pas de modification du contenu, respect des quotas et des règles de contenu.
9. **Android** : Android 8.0 (API 26) minimum ; le Picture-in-Picture est supporté à partir d'API 26 mais dépend du fabricant (certains OEM le désactivent).
10. **Pas de téléchargement** : l'application ne propose **aucun** téléchargement de vidéo, aucun export audio/vidéo, aucun contournement de la publicité.

---

## 9. Confidentialité et données collectées

* **Aucune** télémétrie, aucun analytics, aucun crash reporting distant.
* **Aucun** compte utilisateur, aucun identifiant persistant transmis à YouTube par l'application.
* **Favoris, historique, recherches récentes** : stockés exclusivement sur l'appareil (Room + `SharedPreferences`).
* **Clé API** : chiffrée au repos ; absente de l'appareil en mode proxy.
* **Permissions** : `INTERNET` et `ACCESS_NETWORK_STATE` uniquement.
* `android:allowBackup="false"` : aucune sauvegarde cloud des données locales.

---

## 10. Dépannage

| Symptôme | Cause probable | Solution |
|---|---|---|
| « Aucune clé API configurée » | clé absente | Réglages → saisir la clé, ou activer le proxy |
| « Quota épuisé » | 10 000 unités/jour consommées | attendre le reset (minuit PT) ou utiliser le proxy |
| « Connexion impossible » | réseau / DNS | vérifier la connectivité, le proxy, la certification HTTPS |
| Lecture noire dans l'écran de lecture | embed indisponible pour cette vidéo | ouvrir la vidéo dans l'application YouTube (bouton dédié) |
| `INSTALL_FAILED_UPDATE_INCOMPATIBLE` | signature différente | désinstaller l'ancienne version avant réinstaller |
| L'APK refuse de démarrer sur API 26 | `minSdk` | l'APK du dépôt est compilé en `minSdk 26` : Android 8.0 minimum |

---

## Licence et mentions

Ce projet est fourni « tel quel », à titre de démonstration technique. YouTube est une marque de
Google LLC ; cette application n'est ni affiliée ni approuvée par Google. L'utilisation de l'API
YouTube Data API v3 est soumise aux
[Conditions d'utilisation de l'API YouTube](https://developers.google.com/youtube/terms/developer-policies)
et aux [Règles de la communauté YouTube](https://www.youtube.com/static?template=terms).
