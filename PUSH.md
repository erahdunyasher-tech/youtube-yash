# Pousser le projet sur GitHub

Le dépôt local est prêt (105 fichiers, 2 commits, aucun secret dans l'arbre).
Reste à overcoming l'authentification GitHub.

---

## 🧩 Diagnostic de l'erreur actuelle

```
remote: Permission to erahdunyasher-tech/Youtube.git denied to erahdunyasher-tech.
fatal: unable to access '.../Youtube.git/': The requested URL returned error: 403
```

**Cause** : le jeton utilisé est un *fine-grained PAT* configuré en
**lecture seule** sur les dépôts publics. Il peut lire l'API (d'où l'affichage
correct des métadonnées) mais **ne peut pas écrire** : il lui manque la
permission *Contents → Read and write* sur le dépôt ciblé.

Erreurs API liées (toutes dues au même manque de droits) :

| Appel | Réponse | Permission requise |
|---|---|---|
| `git push` | 403 | Contents → Read and write |
| `PATCH /repos/…` (description, topics) | 403 | Administration → Read and write |
| `POST /repos/…/pages` | 403 | (non requis : fait par le workflow) |

---

## ✅ Solution A — corriger le jeton (2 minutes, je fais le push)

1. Ouvrir <https://github.com/settings/personal-access-tokens> → cliquer sur le jeton existant.
2. **Repository access** → changer *Public repositories (read-only)* en :
   - **Only select repositories** → cocher `Youtube` (et `youtube-yash` si souhaité), **ou**
   - **All repositories**.
3. **Permissions → Repository permissions** → **Contents : Read and write**
   (+ *Metadata : Read-only*, et *Administration : Read and write* si vous voulez que je
   renseigne description, topics et licence).
4. **Generate new token** →Me donner le nouveau jeton (l'ancien est compromis, il doit
   être **révoqué**).

Le push est ensuite automatique : commit + `git push -u origin main` + description,
topics, licence et activation de GitHub Pages.

---

## ✅ Solution B — pousser depuis votre machine (sans jeton)

Sur le poste où se trouve le projet :

```bash
cd /chemin/vers/youtube-libre

# 1. Identité
git config user.name  "Votre Nom"
git config user.email "votre@email.com"

# 2. Dépôt local + commit (si ce n'est pas déjà fait)
[ -d .git ] || git init -b main
git add -A
git commit -m "feat: YouTube Libre v1.0.0 — app Android Kotlin (Material 3) + docs"

# 3. Remote + push (une invite vous demandera le PAT ou ouvre le navigateur)
git remote remove origin 2>/dev/null
git remote add origin https://github.com/erahdunyasher-tech/Youtube.git
git push -u origin main
```

> Avec la 2FA : utilisez un **PAT** comme mot de passe.
> Sans 2FA mais avec un compte protégé : activer l'authentification par clé SSH
> (`ssh-keygen -t ed25519`, puis `ssh -T git@github.com` pour vérifier).

---

## ✅ Solution C — le script tout-en-un

```bash
./publish-github.sh Youtube public
```

Le script configure l'identité, commite, crée le dépôt si besoin, pousse
(jeton lu au terminal, jamais écrit sur disque) et affiche les étapes suivantes.

---

## 📋 Après le push

| Action | Où |
|---|---|
| Activer le site de doc | `Settings → Pages → Source : GitHub Actions` |
| Badges du README | remplacer `VOTRE-COMPTE` par `erahdunyasher-tech` (2 occurrences) |
| Workflows | onglet **Actions** → `android.yml` (build + APK debug), `web.yml` (Pages) |
| Release APK signée | 4 secrets du dépôt + `git tag v1.0.0 && git push origin v1.0.0` |

---

## 🔐 Hygiène des secrets — à faire maintenant

1. **Révoquer le jeton collé dans le chat** :
   <https://github.com/settings/personal-access-tokens> → *Revoke*.
2. Générer un nouveau jeton (solution A) ou utiliser un PAT local (solution B).
3. Ne jamais commiter : `keystore.properties`, `*.jks`, `*.p12`, `.env*`,
   `google-services.json` — le `.gitignore` les bloque déjà.
4. La clé API YouTube n'appartient **pas** au dépôt : elle reste sur l'appareil
   (keystore) ou dans `YOUTUBE_API_KEY` côté serveur.
