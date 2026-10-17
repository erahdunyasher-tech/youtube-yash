# YT-API — serveur local de recherche (Python / FastAPI)

Outil **local** de test : recherche et métadonnées YouTube derrière une petite API
REST, avec une interface web. Fait partie des outils de diagnostic du projet —
il n'est **pas** consommé par l'application Android.

## Démarrage

```bash
cd ytapi
python3 -m pip install -r requirements.txt     # (venv recommandé)
python3 -m uvicorn main:app --port 8000
```

Puis : <http://127.0.0.1:8000> (interface) · <http://127.0.0.1:8000/docs> (Swagger).

```bash
curl "http://127.0.0.1:8000/health"
curl "http://127.0.0.1:8000/search?q=lofi%20hip%20hop&n=5"
curl "http://127.0.0.1:8000/video?url=https://www.youtube.com/watch?v=n61ULEU7CO0"
```

## Endpoints

| Route | Rôle | Coût |
|---|---|---|
| `GET /search?q=&n=` | recherche, `extract_flat` (1 requête) | 1 requête YouTube, cache 1 h |
| `GET /video?url=` | fiche publique (titre, chaîne, vues, miniature, sous-titres) | 1 requête, cache 1 h |
| `GET /health` | sonde | — |
| `GET /` | interface de test | — |

Réponses : `{"cached": bool, "results": [...]}` — le champ `cached` indique si la
réponse vient du cache mémoire (`cache.py`, TTL 3600 s).

## Ce qui n'est PAS dans cette version — et pourquoi

| Élément | Décision |
|---|---|
| `/stream` (URL de flux direct) | **Exclu** — extraction de flux vidéo : interdit par le cahier des charges du projet |
| `/download` (fichier en `/tmp`) | **Exclu** — téléchargement non autorisé |
| Rotation de proxys (`ProxyRotator`) | **Exclu** — dissimulation d'accès automatisé |
| `android_entry.py` / `buildozer.spec` (Kivy) | **Non livrés** — packaging tronqué, et il embarquerait les endpoints ci-dessus |

Ces choix reprennent le cahier des charges d'origine du projet
(`README.md` § « Limites imposées par YouTube ») : l'application affiche les
vidéos **via le lecteur officiel de YouTube**, sans extraction ni téléchargement.

## À la place, pour la lecture

* **Embed officiel** : `https://www.youtube.com/embed/<ID>` (déjà intégré à
  l'application Android, `PlayerWebView.kt`) — la publicité et les règles de
  diffusion de YouTube s'y appliquent, comme il se doit.
* **Sans aucune clé** : flux RSS `https://www.youtube.com/feeds/videos.xml?channel_id=…`
  et endpoint oEmbed officiel.

## Remarques techniques

* **Comportement observé** (test du 17/10/2026, IP de datacenter) :
  `/health`, `/` et `/search` fonctionnent ; `/video` a renvoyé
  *« Sign in to confirm you're not a bot »* (HTTP 400) — la extraction complète
  déclenche la détection anti-bot de YouTube. **Aucune parade n'est fournie ici**
  (cookies, proxys, jetons) : c'est précisément le type de contournement que ce
  projet s'interdit. Si l'endpoint est bloqué, revenez à l'API officielle
  (`videos.list`, 1 unité) qui n'a pas ce problème.
* `extract_flat=True` : une seule requête, mais `duration`/`views` peuvent être
  `None` selon les entrées ; retirez l'option si vous voulez les détails.
* Sans runtime JavaScript (`deno`), yt-dlp émet un avertissement — sans effet
  sur les métadonnées, mais certains **formats** peuvent manquer.
* yt-dlp est un projet tiers : il casse régulièrement, sans SLA, contrairement
  à la YouTube Data API v3 (100 recherches/jour + 10 000 unités/jour, gratuites).
