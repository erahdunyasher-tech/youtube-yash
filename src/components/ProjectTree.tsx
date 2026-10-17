import { CodeBlock, Section } from "./ui";

const tree = `android/
├── settings.gradle.kts            # repositories, modules
├── build.gradle.kts               # plugins racine (AGP, Kotlin, KSP)
├── gradle.properties
├── gradle/wrapper/                # Gradle 8.7
└── app/
    ├── build.gradle.kts            # dépendances, minSdk 26, targetSdk 34
    ├── proguard-rules.pro
    └── src/
        ├── main/
        │   ├── AndroidManifest.xml  # PiP, deep link ytlibre://video/<id>
        │   ├── java/com/ytlibre/app/
        │   │   ├── YouTubeLibreApp.kt
        │   │   ├── di/AppContainer.kt
        │   │   ├── data/
        │   │   │   ├── local/     # AppDatabase (Room), SecurePreferences
        │   │   │   ├── model/     # Video, Channel, SearchPage
        │   │   │   ├── remote/    # YouTubeDataApi, DTO, Mappers,
        │   │   │   │              # ApiErrorMapper, NetworkModule
        │   │   │   └── repository/# YouTubeRepository, LibraryRepository
        │   │   └── ui/
        │   │       ├── common/     # Adapters, UiState, AppError, AppResult
        │   │       ├── main/       # MainActivity (navigation + PiP)
        │   │       ├── home/ search/ watch/ channel/ library/ settings/
        │   │       └── watch/PlayerWebView.kt
        │   └── res/                # layouts, menu, navigation, thèmes, icônes
        ├── test/                   # FormattersTest, ApiErrorMapperTest,
        │                            # YouTubeRepositoryTest
        └── androidTest/            # LibraryDaoTest, MainActivitySmokeTest

server/proxy.mjs                   # proxy Data API : clé côté serveur
README.md                          # configuration, build, tests, limites`;

export default function ProjectTree() {
  return (
    <Section
      id="projet"
      eyebrow="Code source"
      title="Un projet complet, lisible, prêt à compiler"
      description="Chaque couche a une responsabilité claire, les ViewModels ne touchent jamais au réseau, et les accès disque passent par les repositories."
    >
      <CodeBlock label="Arborescence" code={tree} />
    </Section>
  );
}
