import { CodeBlock, Section } from "./ui";

const layers = [
  {
    name: "UI — Fragments XML + ViewBinding",
    detail:
      "Home · Search · Watch · Channel · Library · Settings. Collecte des StateFlow via repeatOnLifecycle, Material 3, RecyclerView adaptateurs (liste et grille).",
    tone: "bg-brand-600/15 border-brand-600/40 text-brand-400",
  },
  {
    name: "ViewModels — androidx.lifecycle",
    detail:
      "UiState<Idle | Loading | Success | Error>, actions déclaratives, debounce 400 ms sur la recherche, pagination, aucun accès réseau direct.",
    tone: "bg-indigo-500/15 border-indigo-500/40 text-indigo-300",
  },
  {
    name: "Repositories",
    detail:
      "YouTubeRepository (Data API v3, cache mémoire TTL 5 min, enrichissement des durées/vues) et LibraryRepository (favoris et historique, 100 % locaux).",
    tone: "bg-emerald-500/15 border-emerald-500/40 text-emerald-300",
  },
  {
    name: "Sources de données",
    detail:
      "Retrofit + OkHttp (cache HTTP 10 Mo, intercepteurs User-Agent et Cache-Control) · Room (SQLite) · EncryptedSharedPreferences (keystore Android, AES-256-GCM).",
    tone: "bg-amber-500/15 border-amber-500/40 text-amber-300",
  },
];

const stack = [
  ["Langage", "Kotlin 1.9.24 · JVM target 17"],
  ["Build", "AGP 8.5.2 · Gradle 8.7 (wrapper) · KSP"],
  ["minSdk / targetSdk", "26 (Android 8.0) / 34"],
  ["UI", "Material 3 · ViewBinding · Navigation Component"],
  ["Asynchrone", "Kotlin Coroutines · Flow / StateFlow"],
  ["Réseau", "Retrofit 2.11 · OkHttp 4.12 · Gson"],
  ["Persistance", "Room 2.6.1 · EncryptedSharedPreferences"],
  ["Images", "Coil 2.6 (miniatures, cache disque)"],
  ["Tests", "JUnit 4 · MockWebServer · Espresso · Room testing"],
];

export default function Architecture() {
  return (
    <Section
      id="architecture"
      eyebrow="Architecture"
      title="Un projet Kotlin clair et maintenable"
      description="Architecture en couches, injection de dépendances manuelle, aucun framework superflu. Chaque couche est testable isolément."
    >
      <div className="grid gap-8 lg:grid-cols-[1.1fr_0.9fr]">
        <div className="space-y-3">
          {layers.map((layer, index) => (
            <div key={layer.name}>
              <div
                className={`rounded-xl border p-5 ${layer.tone.replace("text-", "text-")}`}
              >
                <p className="text-sm font-semibold">{layer.name}</p>
                <p className="mt-1.5 text-sm leading-relaxed text-slate-300/90">
                  {layer.detail}
                </p>
              </div>
              {index < layers.length - 1 ? (
                <div className="flex justify-center py-1 text-slate-600">
                  <svg viewBox="0 0 24 24" className="h-5 w-5" fill="none" stroke="currentColor" strokeWidth={2}>
                    <path d="M12 5v14m0 0-5-5m5 5 5-5" strokeLinecap="round" strokeLinejoin="round" />
                  </svg>
                </div>
              ) : null}
            </div>
          ))}
        </div>

        <div className="space-y-6">
          <div className="overflow-hidden rounded-xl border border-line">
            <table className="w-full text-left text-sm">
              <tbody className="divide-y divide-line">
                {stack.map(([key, value]) => (
                  <tr key={key} className="bg-ink-900">
                    <td className="w-1/3 px-4 py-2.5 font-medium text-slate-300">{key}</td>
                    <td className="px-4 py-2.5 font-mono text-[13px] text-slate-400">{value}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          <CodeBlock
            label="Choix du lecteur officiel"
            code={`// La YouTube Android Player API est dépréciée par Google.
// Voie officiellement recommandée : la page /embed pilotée par l'IFrame Player API.

val url = "https://www.youtube.com/embed/" + videoId +
    "?enablejsapi=1&playsinline=1&rel=0&modestbranding=1" +
    "&origin=https://www.youtube.com&autoplay=1"

// Pont JavaScript : onStateChange -> pause / lecture
// postMessage({event:"command", func:"playVideo", args:[]}) -> lecteur officiel`}
          />
        </div>
      </div>
    </Section>
  );
}
