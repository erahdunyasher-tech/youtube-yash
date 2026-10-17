"""
YT-API — serveur local **conforme**, branché sur la YouTube Data API v3 officielle.

Routes (inchangées par rapport à la version précédente) :

  GET /search?q=&n=&order=   recherche de vidéos (search.list + videos.list)
  GET /video?url=            fiche publique d'une vidéo (videos.list)
  GET /health                sonde : clé configurée, cache, version
  GET /                      interface de test

Configuration :
  cp .env.example .env       puis renseignez YOUTUBE_API_KEY=AIza…

Coût (quotas Google, gratuits) :
  search.list  1 unité (bac à part : 100 recherches/jour)
  videos.list  1 unité (bac partagé : 10 000 unités/jour)
  → une recherche = 1 recherche + 1 unité d'enrichissement (50 vidéos max)

Exclus volontairement : extraction de flux, téléchargement, rotation de proxys.
"""

from __future__ import annotations

import os
import re

import httpx
from fastapi import FastAPI, HTTPException, Query
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import HTMLResponse
from fastapi.staticfiles import StaticFiles

import cache

try:  # python-dotenv facultatif : l'environnement suffit
    from dotenv import load_dotenv

    load_dotenv()
except ImportError:  # pragma: no cover
    pass

# Sureau = API officielle ; surcharge possible pour tests (mock local).
API_BASE = os.getenv(
    "YOUTUBE_API_BASE", "https://www.googleapis.com/youtube/v3/"
).rstrip("/") + "/"
TIMEOUT = 20.0

app = FastAPI(title="YT-API", version="2.0")
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["GET"],
    allow_headers=["*"],
)

STATIC_DIR = os.path.join(os.path.dirname(__file__), "static")
if os.path.isdir(STATIC_DIR):
    app.mount("/static", StaticFiles(directory=STATIC_DIR), name="static")


# ---------------------------------------------------------------------------
# Utilitaires
# ---------------------------------------------------------------------------

def api_key() -> str:
    key = (os.getenv("YOUTUBE_API_KEY") or os.getenv("GOOGLE_API_KEY") or "").strip()
    if not key:
        raise HTTPException(
            503,
            "Clé API absente : copiez .env.example vers .env puis renseignez "
            "YOUTUBE_API_KEY (ou exportez-la dans l'environnement).",
        )
    return key


def parse_duration(iso: str | None) -> int | None:
    """'PT1H2M3S' -> 3723 (secondes)."""
    if not iso:
        return None
    m = re.fullmatch(
        r"P(?:(\d+)D)?(?:T(?:(\d+)H)?(?:(\d+)M)?(?:(\d+)S)?)?", iso
    )
    if not m:
        return None
    d, h, mn, s = (int(g) if g else 0 for g in m.groups())
    return d * 86400 + h * 3600 + mn * 60 + s


def video_id(url: str) -> str | None:
    """Accepte une URL (watch, youtu.be, embed, shorts) ou un ID brut (11 car.)."""
    url = url.strip()
    if re.fullmatch(r"[\w-]{11}", url):
        return url
    for pat in (
        r"[?&]v=([\w-]{11})",
        r"youtu\.be/([\w-]{11})",
        r"/embed/([\w-]{11})",
        r"/shorts/([\w-]{11})",
        r"/live/([\w-]{11})",
    ):
        m = re.search(pat, url)
        if m:
            return m.group(1)
    return None


def best_thumbnail(thumbs: dict | None) -> str | None:
    if not thumbs:
        return None
    for name in ("maxres", "standard", "high", "medium", "default"):
        url = (thumbs.get(name) or {}).get("url")
        if url:
            return url
    return None


def map_api_error(status: int, payload: dict) -> HTTPException:
    err = (payload or {}).get("error") or {}
    reasons = [e.get("reason") for e in (err.get("errors") or []) if e.get("reason")]
    reason = reasons[0] if reasons else None

    if status == 403 and reason in (
        "quotaExceeded",
        "dailyLimitExceeded",
        "rateLimitExceeded",
        "userRateLimitExceeded",
    ):
        return HTTPException(
            429,
            "Quota de l'API YouTube épuisé. Il est remis à zéro à minuit "
            "(heure du Pacifique) — voyez ytapi/README.md pour l'augmenter.",
        )
    if status in (400, 401, 403):
        detail = err.get("message") or ""
        return HTTPException(
            status,
            f"Clé API refusée ({reason or status}). Vérifiez la clé, les restrictions "
            f"d'application, et que « YouTube Data API v3 » est bien activée. {detail}".strip(),
        )
    if status >= 500:
        return HTTPException(502, "Service YouTube momentanément indisponible.")
    return HTTPException(status, err.get("message") or f"Erreur API ({status}).")


async def yt_get(path: str, **params) -> dict:
    """Appel GET à la Data API v3 + traduction des erreurs en messages actionnables."""
    try:
        async with httpx.AsyncClient(timeout=TIMEOUT) as client:
            resp = await client.get(API_BASE + path, params=params)
    except httpx.TimeoutException:
        raise HTTPException(504, "L'API YouTube n'a pas répondu à temps (réseau lent).")
    except httpx.HTTPError as exc:
        raise HTTPException(502, f"Connexion impossible à l'API YouTube : {exc}")

    try:
        payload = resp.json()
    except ValueError:
        raise HTTPException(502, "Réponse illisible de l'API YouTube.")

    if resp.status_code != 200:
        raise map_api_error(resp.status_code, payload)
    return payload


def to_int(value) -> int | None:
    try:
        return int(value)
    except (TypeError, ValueError):
        return None


# ---------------------------------------------------------------------------
# Routes
# ---------------------------------------------------------------------------

@app.get("/", response_class=HTMLResponse)
def index():
    idx = os.path.join(STATIC_DIR, "index.html")
    if os.path.exists(idx):
        with open(idx, encoding="utf-8") as f:
            return f.read()
    return "<h1>YT-API</h1><p>Interface non trouvée.</p>"


@app.get("/search")
async def search(
    q: str,
    n: int = Query(10, ge=1, le=50),
    order: str = Query("relevance", pattern="^(relevance|date|viewCount|rating)$"),
):
    """Recherche de vidéos : 1 appel search.list + 1 appel videos.list (durée/vues)."""
    key = api_key()
    cache_key = f"search:{q}:{n}:{order}"
    cached = cache.get(cache_key)
    if cached:
        return {"cached": True, "results": cached}

    data = await yt_get(
        "search",
        part="snippet",
        type="video",
        q=q,
        maxResults=n,
        order=order,
        safeSearch="moderate",
        key=key,
    )

    items = [
        item for item in data.get("items") or []
        if (item.get("id") or {}).get("videoId")
    ]
    ids = [item["id"]["videoId"] for item in items]

    # Enrichissement en un seul appel (50 IDs max = 1 unité) : durée + vues.
    details: dict = {}
    if ids:
        info = await yt_get(
            "videos",
            part="contentDetails,statistics",
            id=",".join(ids[:50]),
            key=key,
        )
        details = {v.get("id"): v for v in info.get("items") or []}

    results = []
    for item in items:
        vid = item["id"]["videoId"]
        snip = item.get("snippet") or {}
        det = details.get(vid) or {}
        results.append(
            {
                "id": vid,
                "title": snip.get("title"),
                "url": f"https://www.youtube.com/watch?v={vid}",
                "duration": parse_duration((det.get("contentDetails") or {}).get("duration")),
                "channel": snip.get("channelTitle"),
                "views": to_int((det.get("statistics") or {}).get("viewCount")),
                "thumbnail": best_thumbnail(snip.get("thumbnails"))
                or f"https://i.ytimg.com/vi/{vid}/mqdefault.jpg",
            }
        )

    cache.set(cache_key, results)
    return {"cached": False, "results": results}


@app.get("/video")
async def video(url: str):
    """Fiche publique d'une vidéo : métadonnées uniquement (aucun format, aucun flux)."""
    key = api_key()
    vid = video_id(url)
    if not vid:
        raise HTTPException(400, "URL ou identifiant de vidéo illisible (11 caractères attendu).")

    cache_key = f"video:{vid}"
    cached = cache.get(cache_key)
    if cached:
        return {"cached": True, "data": cached}

    info = await yt_get(
        "videos",
        part="snippet,contentDetails,statistics",
        id=vid,
        key=key,
    )
    items = info.get("items") or []
    if not items:
        raise HTTPException(404, "Vidéo introuvable (privée, supprimée ou restreinte).")

    v = items[0]
    snip = v.get("snippet") or {}
    stats = v.get("statistics") or {}
    content = v.get("contentDetails") or {}

    data = {
        "id": vid,
        "title": snip.get("title"),
        "description": (snip.get("description") or "")[:1000],
        "duration": parse_duration(content.get("duration")),
        "channel": snip.get("channelTitle"),
        "views": to_int(stats.get("viewCount")),
        "likes": to_int(stats.get("likeCount")),
        "upload_date": (snip.get("publishedAt") or "")[:10].replace("-", ""),
        "published_at": snip.get("publishedAt"),
        "thumbnail": best_thumbnail(snip.get("thumbnails")),
        "webpage_url": f"https://www.youtube.com/watch?v={vid}",
        "live": (content.get("regionRestriction") is not None)
        or snip.get("liveBroadcastContent") == "live",
    }

    cache.set(cache_key, data)
    return {"cached": False, "data": data}


@app.get("/health")
def health():
    key = (os.getenv("YOUTUBE_API_KEY") or os.getenv("GOOGLE_API_KEY") or "").strip()
    return {
        "status": "ok",
        "api": "youtube-data-api-v3",
        "key_configured": bool(key),
        # Seuls les 6 derniers caractères servent à vérifier qu'une clé est chargée.
        "key_hint": ("…" + key[-6:]) if key else None,
        "cache_entries": len(cache._store),
    }
