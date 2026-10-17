/**
 * Proxy YouTube Data API v3 — serveur de référence (Node.js 18+, sans dépendance).
 *
 * Rôle : conserver la clé API côté serveur (variable d'environnement), afin
 * qu'aucune clé ne soit embarquée dans l'APK, et atténuer la consommation de
 * quota grâce à un cache mémoire court.
 *
 * Lancement :
 *   YOUTUBE_API_KEY=... PORT=8080 node server/proxy.mjs
 *
 * côté Android (Réglages > Proxy serveur) :
 *   https://votre-domaine.example/youtube/v3/
 *
 * Le proxy relaie uniquement les endpoints read-only de la Data API v3
 * (search, videos, channels). Aucune donnée n'est journalisée ni conservée
 * au-delà du cache en mémoire.
 */
import { createServer } from 'node:http';

const API_KEY = process.env.YOUTUBE_API_KEY;
const PORT = Number(process.env.PORT || 8080);
const UPSTREAM = 'https://www.googleapis.com/youtube/v3';
const CACHE_TTL_MS = Number(process.env.CACHE_TTL_MS || 60_000);
const MAX_CACHE_ENTRIES = 500;
const ALLOWED_PATHS = new Set(['/search', '/videos', '/channels']);

if (!API_KEY) {
  console.error('[ytlibre-proxy] Variable d’environnement YOUTUBE_API_KEY manquante.');
  process.exit(1);
}

/** Cache mémoire simple : clé -> { body, expiresAt }. */
const cache = new Map();

function readCache(key) {
  const entry = cache.get(key);
  if (!entry) return null;
  if (entry.expiresAt < Date.now()) {
    cache.delete(key);
    return null;
  }
  return entry.body;
}

function writeCache(key, body) {
  if (cache.size >= MAX_CACHE_ENTRIES) {
    const oldest = cache.keys().next().value;
    if (oldest !== undefined) cache.delete(oldest);
  }
  cache.set(key, { body, expiresAt: Date.now() + CACHE_TTL_MS });
}

function sendJson(res, status, payload) {
  const body = JSON.stringify(payload);
  res.writeHead(status, {
    'Content-Type': 'application/json; charset=utf-8',
    'Access-Control-Allow-Origin': '*',
    'Access-Control-Allow-Headers': 'Content-Type',
    'Cache-Control': 'no-store',
    'Content-Length': Buffer.byteLength(body)
  });
  res.end(body);
}

const server = createServer(async (req, res) => {
  if (req.method === 'OPTIONS') {
    res.writeHead(204, {
      'Access-Control-Allow-Origin': '*',
      'Access-Control-Allow-Headers': 'Content-Type',
      'Access-Control-Allow-Methods': 'GET,OPTIONS'
    });
    res.end();
    return;
  }

  if (req.method !== 'GET') {
    sendJson(res, 405, { error: { code: 405, message: 'Méthode non autorisée.' } });
    return;
  }

  const url = new URL(req.url, `http://${req.headers.host || 'localhost'}`);
  const path = url.pathname.replace(/^\/youtube\/v3/, '');

  if (!ALLOWED_PATHS.has(path)) {
    sendJson(res, 404, { error: { code: 404, message: 'Endpoint non autorisé par le proxy.' } });
    return;
  }

  // La clé fournie par le client est ignorée : c'est celle du serveur qui est utilisée.
  url.searchParams.delete('key');
  url.searchParams.set('key', API_KEY);

  const upstreamUrl = `${UPSTREAM}${path}?${url.searchParams.toString()}`;
  const cacheKey = `${path}?${url.searchParams.toString()}`;

  const cached = readCache(cacheKey);
  if (cached) {
    res.writeHead(200, {
      'Content-Type': 'application/json; charset=utf-8',
      'Access-Control-Allow-Origin': '*',
      'X-YTLibre-Cache': 'HIT'
    });
    res.end(cached);
    return;
  }

  try {
    const upstream = await fetch(upstreamUrl, {
      headers: { 'User-Agent': 'YouTubeLibreProxy/1.0' }
    });
    const body = await upstream.text();

    if (upstream.ok) writeCache(cacheKey, body);

    res.writeHead(upstream.status, {
      'Content-Type': 'application/json; charset=utf-8',
      'Access-Control-Allow-Origin': '*',
      'X-YTLibre-Cache': 'MISS'
    });
    res.end(body);
  } catch (error) {
    sendJson(res, 502, {
      error: { code: 502, message: 'Service YouTube injoignable.', status: 'UPSTREAM_UNAVAILABLE' }
    });
  }
});

server.listen(PORT, () => {
  console.log(`[ytlibre-proxy] à l'écoute sur le port ${PORT} (cache ${CACHE_TTL_MS} ms)`);
});
