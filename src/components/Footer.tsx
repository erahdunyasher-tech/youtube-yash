export default function Footer() {
  return (
    <footer className="border-t border-line bg-ink-900">
      <div className="mx-auto max-w-6xl px-5 py-12 sm:px-8">
        <div className="flex flex-col gap-8 sm:flex-row sm:items-start sm:justify-between">
          <div className="max-w-md">
            <div className="flex items-center gap-2.5">
              <span className="flex h-8 w-8 items-center justify-center rounded-lg bg-brand-600">
                <svg viewBox="0 0 24 24" className="h-4 w-4 fill-white">
                  <path d="M8 5v14l11-7z" />
                </svg>
              </span>
              <span className="text-sm font-semibold tracking-tight text-white">
                YouTube <span className="text-brand-500">Libre</span>
              </span>
            </div>
            <p className="mt-4 text-sm leading-relaxed text-slate-500">
              Application Android native (Kotlin, Material 3) distributable librement.
              Code source complet, sans publicité ajoutée, sans abonnement, sans
              télémétrie. Lecture assurée par le lecteur officiel intégré par YouTube.
            </p>
          </div>

          <div className="grid gap-8 sm:grid-cols-3">
            <div>
              <p className="text-xs font-semibold uppercase tracking-wider text-slate-500">
                Documentation
              </p>
              <ul className="mt-3 space-y-2 text-sm text-slate-400">
                <li><a href="#api" className="hover:text-white">Configuration API</a></li>
                <li><a href="#build" className="hover:text-white">Générer l’APK</a></li>
                <li><a href="#tests" className="hover:text-white">Tests</a></li>
                <li><a href="#projet" className="hover:text-white">Arborescence</a></li>
              </ul>
            </div>
            <div>
              <p className="text-xs font-semibold uppercase tracking-wider text-slate-500">
                Projet
              </p>
              <ul className="mt-3 space-y-2 text-sm text-slate-400">
                <li><a href="#architecture" className="hover:text-white">Architecture</a></li>
                <li><a href="#limites" className="hover:text-white">Limites YouTube</a></li>
                <li><a href="#fonctionnalites" className="hover:text-white">Fonctionnalités</a></li>
              </ul>
            </div>
            <div>
              <p className="text-xs font-semibold uppercase tracking-wider text-slate-500">
                Stack
              </p>
              <ul className="mt-3 space-y-2 text-sm text-slate-400">
                <li>Kotlin · Material 3</li>
                <li>Coroutines · Flow</li>
                <li>Room · Retrofit · Coil</li>
                <li>JUnit · Espresso</li>
              </ul>
            </div>
          </div>
        </div>

        <div className="mt-10 border-t border-line pt-6 text-xs leading-relaxed text-slate-600">
          <p>
            Projet de démonstration fourni « tel quel ». YouTube est une marque de Google LLC ;
            cette application n’est ni affiliée ni approuvée par Google. L’usage de la
            YouTube Data API v3 est soumis aux conditions d’utilisation de l’API et aux règles
            de la communauté YouTube. Aucune extraction de flux, aucun contournement de
            publicité, aucun téléchargement : l’application affiche uniquement le lecteur
            officiel de YouTube.
          </p>
        </div>
      </div>
    </footer>
  );
}
