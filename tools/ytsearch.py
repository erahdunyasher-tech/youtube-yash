#!/usr/bin/env python3
"""
ytsearch — recherche YouTube en ligne de commande (OUTIL LOCAL DE TEST).

IMPORTANT — périmètre et limites
--------------------------------
* Cet outil n'est PAS utilisé par l'application Android. Le data layer de
  l'app reste la YouTube Data API v3 (voir android/.../data/remote/), seule
  solution conforme aux conditions d'utilisation de YouTube.
* yt-dlp interroge les endpoints web internes de YouTube, hors API officielle :
  usage à réserver à vos propres tests locaux, à faible cadence.
* Aucun flux n'est extrait ni téléchargé : 'skip_download' est forcé à True.
* yt-dlp est un projet tiers : il casse régulièrement (mises à jour fréquentes)
  et YouTube bloque parfois les adresses IP automatisées — contrairement à
  l'API officielle, aucun service ni quota garanti.

Usage
-----
    python3 tools/ytsearch.py "lofi hip hop" 5          # recherche
    python3 tools/ytsearch.py "lofi hip hop" 5 --flat   # plus rapide
    python3 tools/ytsearch.py --info https://youtu.be/XXXX   # fiche vidéo

Installation :
    python3 -m pip install yt-dlp
"""

from __future__ import annotations

import argparse
import json
import sys

try:
    import yt_dlp
except ImportError:  # pragma: no cover
    sys.exit("yt-dlp est absent :  python3 -m pip install yt-dlp")

# Options : pas de téléchargement, juste les données.
ydl_opts = {
    "quiet": True,
    "no_warnings": True,
    "skip_download": True,   # aucun téléchargement : métadonnées uniquement
    "extract_flat": False,   # True = plus rapide, moins de détails
}


def search_youtube(query: str, max_results: int = 5, extract_flat: bool = False):
    """Retourne une liste de dictionnaires (id, titre, url, durée, chaîne…)."""
    opts = dict(ydl_opts, extract_flat=extract_flat)
    with yt_dlp.YoutubeDL(opts) as ydl:
        info = ydl.extract_info(f"ytsearch{max_results}:{query}", download=False)
        results = []
        for entry in info.get("entries") or []:
            if not entry:
                continue
            results.append(
                {
                    "id": entry.get("id"),
                    "title": entry.get("title"),
                    "url": entry.get("webpage_url")
                    or f"https://www.youtube.com/watch?v={entry.get('id')}",
                    "duration": entry.get("duration"),
                    "channel": entry.get("uploader") or entry.get("channel"),
                    "views": entry.get("view_count"),
                    "thumbnail": entry.get("thumbnail"),
                }
            )
        return results


def get_video_info(url: str) -> dict:
    """Fiche complète d'une vidéo (ou playlist) — toujours sans téléchargement."""
    with yt_dlp.YoutubeDL(ydl_opts) as ydl:
        return ydl.extract_info(url, download=False)


def format_duration(seconds) -> str:
    if not seconds:
        return "—"
    seconds = int(seconds)
    h, rem = divmod(seconds, 3600)
    m, s = divmod(rem, 60)
    return f"{h}:{m:02d}:{s:02d}" if h else f"{m}:{s:02d}"


def main() -> int:
    parser = argparse.ArgumentParser(description="Recherche YouTube (test local)")
    parser.add_argument("query", nargs="?", help="terme de recherche")
    parser.add_argument(
        "count",
        nargs="?",
        type=int,
        default=None,
        help="nombre de résultats (équivalent de -n), ex. « ytsearch.py lofi 3 »",
    )
    parser.add_argument("-n", "--max", type=int, default=5, help="nombre de résultats")
    parser.add_argument(
        "--flat",
        action="store_true",
        help="extract_flat : 1 requête au lieu de N (durée/vues absentes)",
    )
    parser.add_argument("--info", metavar="URL", help="fiche complète d'une URL")
    parser.add_argument("--json", action="store_true", help="sortie JSON brute")
    args = parser.parse_args()

    if not args.query and not args.info:
        parser.error("indiquez une requête ou --info URL")
    max_results = args.count if args.count is not None else args.max

    try:
        if args.info:
            data = get_video_info(args.info)
            if args.json:
                print(json.dumps(data, ensure_ascii=False, indent=2))
            else:
                print(f"{data.get('title')}  —  {data.get('channel') or data.get('uploader')}")
                print(f"{data.get('webpage_url') or args.info}")
                print(f"vues : {data.get('view_count')} | durée : {format_duration(data.get('duration'))}")
            return 0

        results = search_youtube(args.query, max_results, extract_flat=args.flat)
        if args.json:
            print(json.dumps(results, ensure_ascii=False, indent=2))
            return 0
        for i, v in enumerate(results, 1):
            print(f"{i}. {v['title']}")
            print(f"   {v['channel'] or '?'} · {format_duration(v['duration'])}"
                  f" · {v['views'] if v['views'] is not None else '?'} vues")
            print(f"   {v['url']}")
        print(f"\n{len(results)} résultat(s)")
        return 0

    except yt_dlp.utils.DownloadError as exc:
        print(f"ERREUR yt-dlp : {exc}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
