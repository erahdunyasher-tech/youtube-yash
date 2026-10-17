# YT-API — serveur local conforme (YouTube Data API v3)

API REST Python/FastAPI + interface web pour **rechercher des vidéos et lire leurs
métadonnées**, sans extraction de flux ni téléchargement.

Depuis la version 2.0, ce serveur parle à **l'API officielle YouTube Data API v3**
(zéro blocage anti-bot, quotas documentés). La version antérieure s'appuyait sur
`yt-dlp` : elle reste disponible sous forme de CLI dans `../tools/ytsearch.py`.

> Fait partie des outils de diagnostic : l'application Android, elle, conserve son
> propre data layer Kotlin (`android/.../data/remote/`).

## Démarrage

```bash
cd ytapi
python3 -m venv .venv && source .venv/bin/activate
python3 -m pip install -r requirements.txt

cp .env.example .env        # puis collez votre clé : YOUTUBE_API_KEY=AIza...
python3 -m uvicorn main:app --port 8000
```

* Interface : <http://127.0.0.1:8000>
* Swagger : <http://127.0.0.1:8000/docs>
* Sonde : <http://127.0.0.1:8000/health>

```bash
curl "http://127.0.0.1:8000/search?q=lofi%20hip%20hop&n=5"
curl "http://127.0.0.1:8000/search?q=kotlin&n=5&order=date"
curl "http://127.0.0.1:8000/video?url=https://www.youtube.com/watch?v=n61ULEU7CO0"
curl "http://127.0.0.1:8000/health"
```

## Endpoints et coût (quotas gratuits)

| Route | Appels API | Coût |
|---|---|---|
| `GET /search?q=&n=&order=` | `search.list` + `videos.list` (50 IDs) | 1 recherche (bac 100/jour) + **1 unité** |
| `GET /video?url=` | `videos.list` | **1 unité** |
| `GET /health`, `GET /` | aucun appel | — |

Réponses : `{"cached": bool, "results" | "data": …}` — cache mémoire 1 h (`cache.py`).

`order` ∈ `relevance · date · viewCount · rating` (défaut `relevance`).

## Clé API

1. **https://console.cloud.google.com** → projet → *APIs et services → Bibliothèque*
   → **YouTube Data API v3 → Activer**
2. *Identifiants → Créer des identifiants → Clé API*
3. Restreindre : API *YouTube Data API v3* + package Android
   (`com.ytlibre.app` et `com.ytlibre.app.debug`)
4. `cp .env.example .env`, coller la clé, sauvegarder (`.env` est **ignoré par git**)

Sans clé, `/search` et `/video` renvoient **503** avec le message de configuration ;
`/health` affiche `key_configured: false`.

## Erreurs traduites

| HTTP | Signification | Remède |
|---|---|---|
| 503 | clé absente | remplir `.env` |
| 403 | clé refusée / API non activée / restriction | vérifier clé, restrictions, activation |
| 429 | `quotaExceeded`, `dailyLimitExceeded` | reset à minuit (Pacifique) ; sinon formulaire d'audit (voir plus bas) |
| 504 / 502 | réseau / service indisponible | réessayer |
| 404 | vidéo privée/supprimée | — |

## Ce qui n'est PAS dans ce serveur — et pourquoi

| Élément | Décision |
|---|---|
| `/stream` (URL de flux direct) | **Exclu** — extraction de flux : interdit par le cahier des charges du projet |
| `/download` (fichier puis servi) | **Exclu** — téléchargement non autorisé |
| Rotation de proxys | **Exclu** — dissimulation d'accès automatisé |
| Contournement anti-bot (cookies, jetons…) | **Non fourni** — l'API officielle n'en a pas besoin |
| `android_entry.py` / `buildozer.spec` (Kivy) | **Non livrés** — packaging tronqué et obsolète (app Kotlin existante) |

Pour la lecture, l'application utilise l'**embed officiel** de YouTube
(`PlayerWebView.kt`) : la publicité et les règles de diffusion de YouTube
s'appliquent normalement.

## Augmenter le quota au-delà de 100 recherches/jour

Google ne vend pas de quota. Seule voie officielle :
[YouTube API Services — Audit and Quota Extension Form](https://support.google.com/youtube/contact/yt_api_form)
(audit de conformité, revue de plusieurs semaines, non garanti).
Créer plusieurs projets pour additionner les quotas viole les conditions de l'API.

## Tests effectués

* sans clé → `/health` `key_configured:false`, `/search` **503** avec instruction ;
* avec clé factice → 403 traduit en message clair ;
* parcours complet (recherche + enrichissement + fiche) vérifié contre un
  **amont simulé** (`YOUTUBE_API_BASE`) pour valider le mapping sans consommer
  de quota ;
* `/stream` et `/download` → **404** (non implémentés).
