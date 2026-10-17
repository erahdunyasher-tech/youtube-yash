# Contribuer

Merci pour votre intérêt pour YouTube Libre !

## Environnement

| Outil | Version |
|---|---|
| JDK | 17 |
| Android Studio | Koala 2024.1+ |
| Android SDK | API 34 (compile) / API 26 minimum |
| Node.js | 18+ (site de documentation) |

## Démarrage

```bash
git clone https://github.com/<votre-compte>/youtube-libre.git
cd youtube-libre

# Application Android
cd android
gradle wrapper --gradle-version 8.7   # si gradlew est absent
./gradlew assembleDebug

# Site de documentation
npm install
npm run dev
```

## Avant d'ouvrir une pull request

```bash
cd android
./gradlew clean build          # compilation + tests unitaires + lint
./gradlew connectedAndroidTest # tests instrumentés (appareil/émulateur requis)
```

* Respectez la structure existante (UI / ViewModel / Repository / Data).
* Ajoutez un test unitaire pour tout nouveau mapping ou cas d'erreur.
* Documentez toute limite technique imposée par YouTube dans `README.md`.
* Ne committez aucune clé d'API ni fichier de signature.

## Règles de conception non négociables

1. Aucun téléchargement, aucune extraction de flux, aucun contournement de la
   publicité ou des règles de diffusion de YouTube.
2. Aucune promesse de lecture en arrière-plan ou sans publicité : ces
   fonctionnalités ne sont pas prises en charge officiellement par YouTube.
3. Collecte de données minimale : favoris et historique restent sur l'appareil.
