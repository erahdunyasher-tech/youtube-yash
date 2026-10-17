"""
YT-API — serveur local de **recherche et de métadonnées** YouTube.

Périmètre volontairement restreint (voir ytapi/README.md) :

  INCLUS   /search  recherche (extract_flat, métadonnées seules)
           /video   fiche d'une vidéo (titre, chaîne, vues, miniature…)
           /health  sonde de disponibilité

  EXCLUS   /stream   extraction d'URL de flux vidéo
           /download téléchargement de fichiers
           rotation de proxys

L'application Android, elle, continue d'utiliser la YouTube Data API v3
(ffmpeg : android/app/src/main/java/com/ytlibre/app/data/remote/).

Usage :  python3 -m uvicorn main:app --port 8000
"""

from __future__ import annotations

import os

import yt_dlp
from fastapi import FastAPI, HTTPException, Query
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import HTMLResponse
from fastapi.staticfiles import StaticFiles

import cache

app = FastAPI(title="YT-API", version="1.0")

# Utile si vous ouvrez l'interface depuis un autre onglet/pixel local.
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["GET"],
    allow_headers=["*"],
)

STATIC_DIR = os.path.join(os.path.dirname(__file__), "static")
if os.path.isdir(STATIC_DIR):
    app.mount("/static", StaticFiles(directory=STATIC_DIR), name="static")


def ydl_opts(extra: dict | None = None) -> dict:
    """Options sans téléchargement, sans proxy, avec timeout."""
    opts = {
        "quiet": True,
        "no_warnings": True,
        "skip_download": True,
        "socket_timeout": 15,
    }
    if extra:
        opts.update(extra)
    return opts


@app.get("/", response_class=HTMLResponse)
def index():
    idx = os.path.join(STATIC_DIR, "index.html")
    if os.path.exists(idx):
        with open(idx, encoding="utf-8") as f:
            return f.read()
    return "<h1>YT-API</h1><p>Interface non trouvée.</p>"


@app.get("/search")
def search(q: str, n: int = Query(10, ge=1, le=50)):
    """Recherche : métadonnées uniquement (extract_flat = 1 requête)."""
    key = f"search:{q}:{n}"
    cached = cache.get(key)
    if cached:
        return {"cached": True, "results": cached}

    try:
        with yt_dlp.YoutubeDL(ydl_opts({"extract_flat": True})) as ydl:
            info = ydl.extract_info(f"ytsearch{n}:{q}", download=False)
    except yt_dlp.utils.DownloadError as e:
        raise HTTPException(502, f"échec de la recherche : {e}")

    results = [
        {
            "id": e.get("id"),
            "title": e.get("title"),
            "url": f"https://www.youtube.com/watch?v={e.get('id')}",
            "duration": e.get("duration"),
            "channel": e.get("uploader") or e.get("channel"),
            "views": e.get("view_count"),
            "thumbnail": f"https://i.ytimg.com/vi/{e.get('id')}/mqdefault.jpg",
        }
        for e in info.get("entries") or []
        if e and e.get("id")
    ]

    cache.set(key, results)
    return {"cached": False, "results": results}


@app.get("/video")
def video(url: str):
    """Fiche d'une vidéo : métadonnées publiques, SANS liste de formats ni URL de flux."""
    key = f"video:{url}"
    cached = cache.get(key)
    if cached:
        return {"cached": True, "data": cached}

    try:
        with yt_dlp.YoutubeDL(ydl_opts()) as ydl:
            info = ydl.extract_info(url, download=False)
    except yt_dlp.utils.DownloadError as e:
        raise HTTPException(400, str(e))

    data = {
        "id": info.get("id"),
        "title": info.get("title"),
        "description": (info.get("description") or "")[:1000],
        "duration": info.get("duration"),
        "channel": info.get("uploader"),
        "views": info.get("view_count"),
        "likes": info.get("like_count"),
        "upload_date": info.get("upload_date"),
        "thumbnail": info.get("thumbnail"),
        "webpage_url": info.get("webpage_url"),
        "subtitles": list((info.get("subtitles") or {}).keys()),
    }
    cache.set(key, data)
    return {"cached": False, "data": data}


@app.get("/health")
def health():
    return {"status": "ok", "yt_dlp": yt_dlp.version.__version__, "cache": len(cache._store)}
