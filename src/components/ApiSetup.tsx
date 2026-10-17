import { Badge, CodeBlock, Section } from "./ui";

const steps = [
  {
    title: "Créer le projet Google Cloud",
    body: "Console Google Cloud → créer un projet, puis APIs et services → Bibliothèque → rechercher « YouTube Data API v3 » → Activer.",
  },
  {
    title: "Créer et restreindre la clé",
    body: "Identifiants → Créer des identifiants → Clé API. Restreindre l’API à « YouTube Data API v3 » et l’application à Android (package com.ytlibre.app + empreinte SHA-1).",
  },
  {
    title: "Choisir le mode de configuration",
    body: "Mode direct : la clé est saisie dans l’application et chiffrée par le keystore. Mode proxy : la clé reste dans l’environnement du serveur, l’appareil n’envoie jamais de paramètre key.",
  },
  {
    title: "Tester puis économiser le quota",
    body: "Le bouton « Tester la connexion » de l’écran Réglages valide la configuration. Le cache mémoire (5 min) et le cache HTTP (10 Mo) limitent les appels répétés.",
  },
];

const errors = [
  ["403 keyInvalid", "Clé incorrecte ou mal copiée", "Régénérer la clé, vérifier l’activation de l’API"],
  ["403 accessNotConfigured", "API non activée", "Activer YouTube Data API v3 dans le projet"],
  ["403 quotaExceeded", "Quota 10 000 unités/jour épuisé", "Attendre le reset, passer par le proxy, réduire la fréquence"],
  ["400 badRequest", "Paramètres invalides", "Vérifier type, q, maxResults et les restrictions de la clé"],
];

export default function ApiSetup() {
  return (
    <Section
      id="api"
      eyebrow="Configuration"
      title="Brancher la YouTube Data API v3 en cinq minutes"
      description="L’application ne contient aucune clé en dur : soit l’utilisateur colle sa clé personnelle (stockée chiffrée), soit un proxy serveur injecte la clé depuis l’environnement."
    >
      <div className="grid gap-8 lg:grid-cols-[1fr_1fr]">
        <ol className="space-y-4">
          {steps.map((step, index) => (
            <li key={step.title} className="flex gap-4 rounded-xl border border-line bg-ink-900 p-5">
              <span className="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg bg-brand-600/15 text-sm font-semibold text-brand-400">
                {index + 1}
              </span>
              <div>
                <p className="text-sm font-semibold text-white">{step.title}</p>
                <p className="mt-1.5 text-sm leading-relaxed text-slate-400">{step.body}</p>
              </div>
            </li>
          ))}

          <div className="rounded-xl border border-line bg-ink-900 p-5">
            <p className="text-sm font-semibold text-white">Erreurs fréquentes</p>
            <div className="mt-4 overflow-hidden rounded-lg border border-line">
              <table className="w-full text-left text-xs">
                <thead className="bg-ink-850 text-slate-500">
                  <tr>
                    <th className="px-3 py-2 font-medium">Erreur</th>
                    <th className="px-3 py-2 font-medium">Cause</th>
                    <th className="px-3 py-2 font-medium">Correction</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-line">
                  {errors.map(([code, cause, fix]) => (
                    <tr key={code} className="bg-ink-900">
                      <td className="px-3 py-2 font-mono text-brand-400">{code}</td>
                      <td className="px-3 py-2 text-slate-400">{cause}</td>
                      <td className="px-3 py-2 text-slate-300">{fix}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </ol>

        <div className="space-y-5">
          <CodeBlock
            label="Vérification en ligne de commande"
            code={`curl "https://www.googleapis.com/youtube/v3/search?part=snippet&q=kotlin&type=video&maxResults=1&key=AIza..."`}
          />

          <CodeBlock
            label="Mode proxy — clé confinée au serveur"
            code={`# Le client ignore toute clé reçue et utilise celle du serveur.
export YOUTUBE_API_KEY=AIza...
export PORT=8080
node server/proxy.mjs

# Côté Android : Réglages > Proxy serveur
# https://votre-domaine.example/youtube/v3/`}
          />

          <CodeBlock
            label="Réglages côté Android"
            code={`// Clé API stockée chiffrée (keystore Android, AES-256-GCM)
val encryptedPrefs = EncryptedSharedPreferences.create(
    context, "ytlibre_secrets", masterKey,
    PrefKeyEncryptionScheme.AES256_SIV,
    PrefValueEncryptionScheme.AES256_GCM
)
encryptedPrefs.edit().putString("youtube_api_key", key).apply()

// En mode proxy, le paramètre "key" n'est jamais envoyé :
val apiKey = if (network.isProxyMode()) null else preferences.apiKey`}
          />

          <div className="flex flex-wrap gap-2">
            <Badge tone="good">Proxy en lecture seule (search, videos, channels)</Badge>
            <Badge tone="good">Cache mémoire 60 s côté serveur</Badge>
            <Badge tone="neutral">Aucun journal des clés</Badge>
            <Badge tone="warn">Quota par défaut : 10 000 unités/jour</Badge>
          </div>
        </div>
      </div>
    </Section>
  );
}
