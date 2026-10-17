import { Badge, CodeBlock, Section } from "./ui";

const unitTests = [
  ["FormattersTest", "Durées ISO-8601, formatage des vues, dates relatives, normalisation des requêtes"],
  ["ApiErrorMapperTest", "Quota épuisé, limite quotidienne, clé invalide, 429 + Retry-After, erreurs 5xx et réseau"],
  ["YouTubeRepositoryTest", "Paramètres envoyés (key, type, q), mapping des DTO, pagination, erreur quota, cache mémoire"],
];

const androidTests = [
  ["LibraryDaoTest", "Insertion et suppression d’un favori, tri, purge de l’historique au-delà de la limite"],
  ["MainActivitySmokeTest", "Démarrage de l’application et affichage de la navigation sans clé API"],
];

const manual = [
  "Recherche : mot-clé → résultats (vidéos + chaînes) → filtre → pagination",
  "Lecture : ouverture d’une vidéo → play/pause → PiP → retour",
  "Favoris : ajout puis retrait → persistance après redémarrage",
  "Historique : 3 vidéos regardées → ordre anti-chronologique → purge",
  "Mode sombre : Réglages → Thème → Sombre → bascule immédiate",
  "Quota : clé invalide ou quota épuisé → message d’erreur dédié",
  "Hors ligne : mode avion → message réseau, aucun crash",
];

export default function Tests() {
  return (
    <Section
      id="tests"
      eyebrow="Qualité"
      title="Les fonctions principales sont testées"
      description="Tests unitaires JVM pour la logique métier et le réseau, tests instrumentés pour la persistance locale et le démarrage de l’application."
    >
      <div className="grid gap-6 lg:grid-cols-2">
        <div className="rounded-xl border border-line bg-ink-900 p-6">
          <div className="flex items-center justify-between">
            <h3 className="text-base font-semibold text-white">Tests unitaires (JVM)</h3>
            <Badge tone="neutral">app/src/test</Badge>
          </div>
          <ul className="mt-5 space-y-4">
            {unitTests.map(([name, coverage]) => (
              <li key={name}>
                <p className="font-mono text-sm text-brand-400">{name}</p>
                <p className="mt-1 text-sm leading-relaxed text-slate-400">{coverage}</p>
              </li>
            ))}
          </ul>
        </div>

        <div className="rounded-xl border border-line bg-ink-900 p-6">
          <div className="flex items-center justify-between">
            <h3 className="text-base font-semibold text-white">Tests instrumentés</h3>
            <Badge tone="neutral">app/src/androidTest</Badge>
          </div>
          <ul className="mt-5 space-y-4">
            {androidTests.map(([name, coverage]) => (
              <li key={name}>
                <p className="font-mono text-sm text-brand-400">{name}</p>
                <p className="mt-1 text-sm leading-relaxed text-slate-400">{coverage}</p>
              </li>
            ))}
          </ul>
        </div>
      </div>

      <div className="mt-6 grid gap-6 lg:grid-cols-[1fr_1fr]">
        <CodeBlock
          label="Exécution"
          code={`cd android

./gradlew test                    # tests unitaires
./gradlew connectedAndroidTest    # tests instrumentés
./gradlew test jacocoTestReport   # couverture

# Rapport HTML :
# android/app/build/reports/tests/testDebugUnitTest/index.html`}
        />

        <div className="rounded-xl border border-line bg-ink-900 p-6">
          <h3 className="text-base font-semibold text-white">Scénarios manuels</h3>
          <ul className="mt-4 space-y-2.5">
            {manual.map((item) => (
              <li key={item} className="flex items-start gap-3 text-sm text-slate-400">
                <span className="mt-1 h-3.5 w-3.5 shrink-0 rounded border border-line bg-ink-850" />
                {item}
              </li>
            ))}
          </ul>
        </div>
      </div>
    </Section>
  );
}
