# Outils de test locaux

Ce dossier contient des **outils de diagnostic destinés à votre machine**.
Ils ne font **pas** partie de l'application Android.

## `ytsearch.py` — recherche YouTube via yt-dlp

```bash
python3 -m pip install yt-dlp

python3 tools/ytsearch.py "lofi hip hop" 5        # 5 résultats, détails complets
python3 tools/ytsearch.py "lofi hip hop" 5 --flat # plus rapide (1 requête)
python3 tools/ytsearch.py --info https://youtu.be/XXXX
python3 tools/ytsearch.py "kotlin" 3 --json       # sortie JSON
```

### Ce que cet outil fait

* interroge les **endpoints web** de YouTube (via le projet tiers `yt-dlp`) ;
* `skip_download=True` : **aucun téléchargement**, métadonnées uniquement ;
* retourne `id`, `title`, `url`, `duration`, `channel`, `views`, `thumbnail`.

### Ce qu'il ne fait pas — et pourquoi

| | |
|---|---|
| Utilisé par l'app Android | **Non.** Le data layer de l'app reste la **YouTube Data API v3** (seule voie conforme) |
| Garanti à long terme | **Non** : yt-dlp casse régulièrement, YouTube bloque certaines IP automatisées |
| Sans quota ni limite | **Non** : pas de quota officiel, mais rate-limiting et blocages possibles |
| Extraction de flux / téléchargement | **Non**, et le projet l'interdit par ailleurs |

> ⚠️ Les conditions d'utilisation de YouTube réservent l'accès automatisé à
> l'API officielle. Cet outil est fourni pour **vos tests personnels** ; toute
> mise en production passerait par l'API officielle et ses conditions.

### Remarques

* Sans runtime JavaScript (`deno`), yt-dlp émet un avertissement et certains
  **formats** peuvent manquer — sans effet sur la recherche de métadonnées.
* `--flat` (`extract_flat`) : une seule requête au lieu d'une par vidéo,
  mais `duration`/`views` deviennent `None`.
* Rapide à tester aussi avec la clé officielle :
  `curl "https://www.googleapis.com/youtube/v3/search?part=snippet&q=test&type=video&maxResults=1&key=VOTRE_CLE"`
