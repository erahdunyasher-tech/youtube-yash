import { useState } from "react";
import { CodeBlock, Section } from "./ui";

const prereqs = [
  ["JDK", "17 (JBR fourni par Android Studio)"],
  ["Android Studio", "Koala 2024.1 ou supérieur"],
  ["Android SDK", "API 34 (compile), API 26 minimum"],
  ["Gradle", "8.7 — téléchargé par le wrapper"],
];

const studio = `1. File > Open… puis choisir le dossier android/
2. Laisser la synchronisation Gradle se terminer
   (le wrapper récupère Gradle 8.7 automatiquement)
3. Build > Build Bundle(s) / APK(s) > Build APK(s)
4. Sortie : android/app/build/outputs/apk/debug/app-debug.apk
5. Installer sur l'appareil :
   adb install -r app/build/outputs/apk/debug/app-debug.apk`;

const cli = `cd android

# Compilation + tests unitaires + lint
./gradlew clean build

# Tests unitaires JVM
./gradlew test

# Tests instrumentés (appareil ou émulateur requis)
./gradlew connectedAndroidTest

# APK debug
./gradlew assembleDebug

# APK release
./gradlew assembleRelease

# Bundle Play Store (AAB)
./gradlew bundleRelease`;

const signing = `keytool -genkey -v -keystore ytl-release.jks \\
  -keyalg RSA -keysize 2048 -validity 10000 -alias ytl

# android/keystore.properties (hors dépôt)
storeFile=../ytl-release.jks
storePassword=…
keyAlias=ytl
keyPassword=…

./gradlew bundleRelease`;

export default function BuildSteps() {
  const [tab, setTab] = useState<"studio" | "cli" | "signing">("studio");

  const tabs = [
    { id: "studio" as const, label: "Android Studio" },
    { id: "cli" as const, label: "Ligne de commande" },
    { id: "signing" as const, label: "Signature release" },
  ];

  return (
    <Section
      id="build"
      eyebrow="Build"
      title="Du code source à l’APK installable"
      description="Le projet est un projet Gradle complet : ouverture directe dans Android Studio, ou build reproductible en CLI."
    >
      <div className="grid gap-8 lg:grid-cols-[0.85fr_1.15fr]">
        <div className="space-y-6">
          <div className="overflow-hidden rounded-xl border border-line">
            <table className="w-full text-left text-sm">
              <tbody className="divide-y divide-line">
                {prereqs.map(([key, value]) => (
                  <tr key={key} className="bg-ink-900">
                    <td className="w-2/5 px-4 py-3 font-medium text-slate-300">{key}</td>
                    <td className="px-4 py-3 text-slate-400">{value}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          <div className="rounded-xl border border-line bg-ink-900 p-5 text-sm leading-relaxed text-slate-400">
            <p className="font-semibold text-white">Compatibilité</p>
            <p className="mt-2">
              minSdk 26 (Android 8.0 Oreo), targetSdk 34. Le Picture-in-Picture requiert
              l’API 26 et un OEM compatible ; les fonctions non supportées sont
              simplement masquées, jamais simulées.
            </p>
          </div>
        </div>

        <div>
          <div className="mb-4 flex flex-wrap gap-2">
            {tabs.map((item) => (
              <button
                key={item.id}
                onClick={() => setTab(item.id)}
                className={`rounded-lg px-3.5 py-2 text-sm font-medium transition-colors ${
                  tab === item.id
                    ? "bg-brand-600 text-white"
                    : "border border-line bg-ink-900 text-slate-400 hover:text-white"
                }`}
              >
                {item.label}
              </button>
            ))}
          </div>

          {tab === "studio" ? <CodeBlock label="Android Studio" code={studio} /> : null}
          {tab === "cli" ? <CodeBlock label="Terminal — dossier android/" code={cli} /> : null}
          {tab === "signing" ? <CodeBlock label="Signature de release" code={signing} /> : null}
        </div>
      </div>
    </Section>
  );
}
