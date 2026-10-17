const links = [
  { href: "#fonctionnalites", label: "Fonctionnalités" },
  { href: "#architecture", label: "Architecture" },
  { href: "#api", label: "API YouTube" },
  { href: "#build", label: "Compilation" },
  { href: "#tests", label: "Tests" },
  { href: "#limites", label: "Limites" },
];

export default function Nav() {
  return (
    <header className="sticky top-0 z-50 border-b border-line/80 bg-ink-950/85 backdrop-blur-xl">
      <nav className="mx-auto flex max-w-6xl items-center justify-between px-5 py-3.5 sm:px-8">
        <a href="#top" className="flex items-center gap-2.5">
          <span className="flex h-8 w-8 items-center justify-center rounded-lg bg-brand-600">
            <svg viewBox="0 0 24 24" className="h-4 w-4 fill-white">
              <path d="M8 5v14l11-7z" />
            </svg>
          </span>
          <span className="text-sm font-semibold tracking-tight text-white">
            YouTube <span className="text-brand-500">Libre</span>
          </span>
        </a>

        <div className="hidden items-center gap-7 md:flex">
          {links.map((link) => (
            <a
              key={link.href}
              href={link.href}
              className="text-sm text-slate-400 transition-colors hover:text-white"
            >
              {link.label}
            </a>
          ))}
        </div>

        <a
          href="#build"
          className="rounded-lg bg-white px-3.5 py-1.5 text-sm font-semibold text-ink-950 transition-colors hover:bg-brand-500 hover:text-white"
        >
          Générer l’APK
        </a>
      </nav>
    </header>
  );
}
