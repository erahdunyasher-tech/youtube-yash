# Politique de sécurité

## Signalement

Si vous détectez une faille de sécurité (clé API exposée, fuite de données
utilisateur, injection, etc.), **n'ouvrez pas d'issue publique**. Signalez-la
via la page « Security » de l'onglet Security du dépôt, ou par e-mail privé au
mainteneur.

Merci d'indiquer : la version, l'appareil, les étapes de reproduction et, si
possible, une preuve d'impact.

## Ce que l'application s'engage à ne jamais faire

* Aucune collecte de données personnelles, aucune télémétrie, aucun analytics.
* Aucun envoi de la clé API à un tiers : elle reste chiffrée sur l'appareil
  (keystore Android) ou dans l'environnement du serveur en mode proxy.
* Aucune extraction de flux vidéo, aucun contournement de publicité, aucun
  téléchargement de contenu protégé.

## Bonnes pratiques pour les contributeurs

* Ne committez **jamais** de fichier `keystore.properties`, `*.jks`, `*.p12` ou
  de clé d'API réelle : le fichier `.gitignore` les exclut déjà.
* Utilisez `server/proxy.mjs` avec la variable d'environnement
  `YOUTUBE_API_KEY` pour les tests manuels.
* Les workflows GitHub Actions lisent les secrets du dépôt
  (`KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`) :
  ils ne sont jamais écrits dans le dépôt.
