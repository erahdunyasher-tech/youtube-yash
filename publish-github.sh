#!/usr/bin/env bash
# =============================================================================
#  YouTube Libre — publication sur GitHub
#
#  ./publish-github.sh [nom-du-repo] [public|private]
#
#  Le script :
#   1. configure l'identité Git (si absente) ;
#   2. initialise le dépôt et crée le commit initial ;
#   3. crée le dépôt GitHub (via `gh` si installé, sinon via l'API) ;
#   4. pousse la branche `main` ;
#   5. configure le remote upstream et affiche les prochaines étapes.
#
#  Le jeton d'accès est lu depuis le terminal (jamais stocké, jamais écrit
#  dans le dépôt, jamais affiché). Aucune clé API YouTube n'est requise ici.
# =============================================================================
set -euo pipefail

REPO_NAME="${1:-youtube-libre}"
VISIBILITY="${2:-public}"
BRANCH="main"
DESCRIPTION="YouTube Libre — application Android gratuite (Kotlin, Material 3) : recherche via la YouTube Data API v3, lecteur officiel intégré, favoris et historique locaux."

cd "$(dirname "$0")"

say()  { printf '\033[1;36m▸ %s\033[0m\n' "$1"; }
ok()   { printf '\033[1;32m✔ %s\033[0m\n' "$1"; }
warn() { printf '\033[1;33m! %s\033[0m\n' "$1"; }
err()  { printf '\033[1;31m✘ %s\033[0m\n' "$1" >&2; exit 1; }

command -v git >/dev/null 2>&1 || err "git est requis sur cette machine."

# --- 1. Identité Git -------------------------------------------------------
if [ -z "$(git config user.name || true)" ] || [ -z "$(git config user.email || true)" ]; then
  say "Identité Git non configurée."
  read -rp "  Nom complet (tel qu'il apparaît sur GitHub) : " GIT_NAME
  read -rp "  E-mail (privé ou noreply@github.com)      : " GIT_EMAIL
  git config user.name "$GIT_NAME"
  git config user.email "$GIT_EMAIL"
  ok "Identité configurée : $GIT_NAME <$GIT_EMAIL>"
fi

# --- 2. Dépôt local + commit initial --------------------------------------
if [ ! -d .git ]; then
  say "Initialisation du dépôt local…"
  git init -b "$BRANCH" >/dev/null
fi

if git diff --cached --quiet 2>/dev/null && git diff --quiet 2>/dev/null; then
  ok "Aucun changement à committer."
else
  git add -A
  if git diff --cached --quiet; then
    ok "Rien à indexer."
  else
    git commit -q -m "feat: YouTube Libre v1.0.0 — app Android Kotlin (Material 3) + docs

- Application Android native (Kotlin, Material 3, minSdk 26) :
  recherche YouTube Data API v3, lecteur officiel embarqué, PiP,
  favoris/historique locaux (Room), mode sombre, gestion du quota.
- Clé API chiffrée (keystore) ou confinée à un proxy serveur (server/proxy.mjs).
- Tests unitaires (JUnit, MockWebServer) et instrumentés (Room, Espresso).
- Site de documentation React + Tailwind + workflows GitHub Actions."
    ok "Commit initial créé."
  fi
fi

# --- 3. Identifiant GitHub + dépôt distant ---------------------------------
read -rp "Votre identifiant GitHub (ex. mon-compte) : " GH_USER
[ -n "$GH_USER" ] || err "Identifiant GitHub manquant."

REMOTE_URL="https://github.com/${GH_USER}/${REPO_NAME}.git"
if git remote get-url origin >/dev/null 2>&1; then
  git remote set-url origin "$REMOTE_URL"
else
  git remote add origin "$REMOTE_URL"
fi
ok "Remote origin : $REMOTE_URL"

# --- 4. Création du dépôt GitHub -------------------------------------------
USE_GH=0
command -v gh >/dev/null 2>&1 && USE_GH=1

if [ "$USE_GH" = "1" ] && gh auth status >/dev/null 2>&1; then
  say "Création du dépôt via GitHub CLI…"
  if gh repo view "${GH_USER}/${REPO_NAME}" >/dev/null 2>&1; then
    warn "Le dépôt ${GH_USER}/${REPO_NAME} existe déjà."
  else
    gh repo create "${GH_USER}/${REPO_NAME}" \
      --"$VISIBILITY" \
      --description "$DESCRIPTION" \
      --source=. --remote=origin --push >/dev/null
    ok "Dépôt créé et poussé via gh."
  fi
else
  say "Création du dépôt via l'API GitHub (jeton personnel requis)."
  warn "Créez un jeton : https://github.com/settings/tokens"
  printf '  Portée suffisante : « repo » (public_repo pour un dépôt public).\n'
  read -rsp "  PAT GitHub (ghp_…) : " TOKEN; echo
  [ -n "$TOKEN" ] || err "Jeton manquant."

  RESPONSE=$(curl -s -w '\n%{http_code}' -X POST \
    -H "Authorization: Bearer ${TOKEN}" \
    -H "Accept: application/vnd.github+json" \
    -H "X-GitHub-Api-Version: 2022-11-28" \
    https://api.github.com/user/repos \
    -d "{\"name\":\"${REPO_NAME}\",\"private\":$([ "$VISIBILITY" = "private" ] && echo true || echo false),\"description\":\"${DESCRIPTION}\",\"has_issues\":true,\"has_wiki\":false}")

  CODE=$(printf '%s' "$RESPONSE" | tail -n1)
  if [ "$CODE" = "201" ]; then
    ok "Dépôt créé sur GitHub."
  elif [ "$CODE" = "422" ]; then
    warn "Le dépôt existe déjà (422) — on continue avec le push."
  else
    err "Création refusée (HTTP $CODE) : $(printf '%s' "$RESPONSE" | head -n1)"
  fi

  # --- 5. Push avec askpass (le jeton n'est jamais écrit sur disque) --------
  ASKPASS="$(mktemp)"
  cat > "$ASKPASS" <<'ASKPASS_EOF'
#!/bin/sh
case "$1" in
  *Username*) echo "x-access-token" ;;
  *Password*) echo "$GIT_TOKEN" ;;
esac
ASKPASS_EOF
  chmod +x "$ASKPASS"

  say "Push de la branche $BRANCH…"
  GIT_TOKEN="$TOKEN" GIT_ASKPASS="$ASKPASS" GIT_TERMINAL_PROMPT=0 \
    git push -u origin "$BRANCH"

  rm -f "$ASKPASS"
  unset TOKEN
  ok "Push terminé."
fi

# --- 6. Upstream + prochaines étapes ---------------------------------------
git remote add upstream "https://github.com/${GH_USER}/${REPO_NAME}.git" 2>/dev/null || true

cat <<EOF

$(ok "Publication prête")

URL du dépôt : https://github.com/${GH_USER}/${REPO_NAME}
Site de documentation (GitHub Pages) :
  Settings → Pages → Source : « GitHub Actions »

APK signé (nécessite 4 secrets du dépôt) :
  Settings → Secrets and variables → Actions → New repository secret
    KEYSTORE_BASE64  →  base64 -w0 ytl-release.jks
    KEYSTORE_PASSWORD, KEY_ALIAS, KEY_PASSWORD
  Puis :  git tag v1.0.0 && git push origin v1.0.0

Rappel : aucune clé API YouTube ne doit être commitée.
EOF
