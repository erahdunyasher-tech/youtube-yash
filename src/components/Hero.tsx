function PhoneMockup() {
  const thumbs = [
    "from-rose-500/40 to-rose-800/20",
    "from-indigo-500/40 to-indigo-800/20",
    "from-emerald-500/40 to-emerald-800/20",
    "from-amber-500/40 to-amber-800/20",
  ];

  return (
    <div className="relative mx-auto w-[280px] rounded-[2.2rem] border border-line bg-ink-900 p-2 shadow-[0_30px_80px_-20px_rgba(255,59,48,0.35)]">
      <div className="absolute left-1/2 top-3 h-1.5 w-16 -translate-x-1/2 rounded-full bg-line" />
      <div className="overflow-hidden rounded-[1.7rem] bg-ink-950">
        <div className="flex items-center gap-2 px-4 pb-3 pt-7">
          <span className="flex h-5 w-5 items-center justify-center rounded bg-brand-600">
            <svg viewBox="0 0 24 24" className="h-3 w-3 fill-white">
              <path d="M8 5v14l11-7z" />
            </svg>
          </span>
          <span className="text-[11px] font-semibold text-white">YouTube Libre</span>
          <span className="ml-auto rounded-md bg-ink-800 px-1.5 py-0.5 text-[9px] text-slate-400">
            FR
          </span>
        </div>

        <div className="flex gap-1.5 overflow-hidden px-3 pb-3">
          {["Tout", "Musique", "Jeux", "Films"].map((chip, i) => (
            <span
              key={chip}
              className={`shrink-0 rounded-full px-2 py-1 text-[9px] ${
                i === 0
                  ? "bg-white text-ink-950"
                  : "bg-ink-800 text-slate-400"
              }`}
            >
              {chip}
            </span>
          ))}
        </div>

        <div className="grid grid-cols-2 gap-2 px-3">
          {thumbs.map((gradient, i) => (
            <div key={i} className="space-y-1.5">
              <div className={`aspect-video rounded-md bg-gradient-to-br ${gradient}`} />
              <div className="h-1.5 w-4/5 rounded-full bg-ink-700" />
              <div className="h-1.5 w-3/5 rounded-full bg-ink-800" />
            </div>
          ))}
        </div>

        <div className="mt-4 flex items-center justify-around border-t border-line px-3 py-3">
          {[0, 1, 2, 3].map((i) => (
            <span
              key={i}
              className={`h-4 w-4 rounded-md ${
                i === 0 ? "bg-brand-500" : "bg-ink-700"
              }`}
            />
          ))}
        </div>
      </div>
    </div>
  );
}

export default function Hero() {
  return (
    <section id="top" className="grid-backdrop relative overflow-hidden">
      <div className="mx-auto grid max-w-6xl items-center gap-14 px-5 py-20 sm:px-8 lg:grid-cols-[1.15fr_0.85fr] lg:py-28">
        <div>
          <div className="flex flex-wrap items-center gap-2 text-xs">
            <span className="inline-flex items-center gap-1.5 rounded-full border border-brand-600/40 bg-brand-600/10 px-3 py-1 font-medium text-brand-400">
              <span className="h-1.5 w-1.5 rounded-full bg-brand-500" />
              Android 8.0+ · Material 3
            </span>
            <span className="rounded-full border border-line bg-ink-800 px-3 py-1 text-slate-400">
              0 € · sans abonnement
            </span>
            <span className="rounded-full border border-line bg-ink-800 px-3 py-1 text-slate-400">
              Aucune publicité ajoutée
            </span>
          </div>

          <h1 className="mt-7 text-4xl font-semibold leading-[1.08] tracking-tight text-white sm:text-5xl lg:text-6xl">
            Regardez YouTube
            <br />
            <span className="bg-gradient-to-r from-brand-400 to-brand-600 bg-clip-text text-transparent">
              sans abonnement.
            </span>
          </h1>

          <p className="mt-6 max-w-xl text-base leading-relaxed text-slate-400 sm:text-lg">
            Une application Android native en Kotlin : recherche de vidéos et de chaînes
            via la YouTube Data API v3, lecture dans le lecteur officiel intégré par
            YouTube, favoris et historiques stockés sur l’appareil, mode sombre et
            Picture-in-Picture.
          </p>

          <div className="mt-9 flex flex-wrap items-center gap-3">
            <a
              href="#build"
              className="rounded-xl bg-brand-600 px-5 py-3 text-sm font-semibold text-white transition-colors hover:bg-brand-500"
            >
              Procédure de compilation
            </a>
            <a
              href="#limites"
              className="rounded-xl border border-line bg-ink-900 px-5 py-3 text-sm font-semibold text-slate-200 transition-colors hover:border-brand-600/50 hover:text-white"
            >
              Ce que YouTube impose
            </a>
          </div>

          <dl className="mt-12 grid max-w-lg grid-cols-3 gap-6 border-t border-line pt-6">
            {[
              ["0", "publicité ajoutée"],
              ["100 %", "données locales"],
              ["0", "télémétrie"],
            ].map(([value, label]) => (
              <div key={label}>
                <dt className="text-2xl font-semibold text-white">{value}</dt>
                <dd className="mt-1 text-xs leading-snug text-slate-500">{label}</dd>
              </div>
            ))}
          </dl>
        </div>

        <div className="relative">
          <div className="absolute -inset-10 -z-10 blur-3xl">
            <div className="mx-auto h-64 w-64 rounded-full bg-brand-600/20" />
          </div>
          <PhoneMockup />
        </div>
      </div>
    </section>
  );
}
