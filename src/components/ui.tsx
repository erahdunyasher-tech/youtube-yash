import type { ReactNode } from "react";

export function Section({
  id,
  eyebrow,
  title,
  description,
  children,
}: {
  id?: string;
  eyebrow: string;
  title: string;
  description?: string;
  children: ReactNode;
}) {
  return (
    <section id={id} className="mx-auto max-w-6xl px-5 py-20 sm:px-8">
      <p className="text-xs font-semibold uppercase tracking-[0.25em] text-brand-500">
        {eyebrow}
      </p>
      <h2 className="mt-3 text-3xl font-semibold tracking-tight text-white sm:text-4xl">
        {title}
      </h2>
      {description ? (
        <p className="mt-4 max-w-2xl text-base leading-relaxed text-slate-400">{description}</p>
      ) : null}
      <div className="mt-12">{children}</div>
    </section>
  );
}

export function CodeBlock({ code, label }: { code: string; label?: string }) {
  return (
    <div className="overflow-hidden rounded-xl border border-line bg-ink-900">
      {label ? (
        <div className="flex items-center gap-2 border-b border-line px-4 py-2 text-xs font-medium text-slate-500">
          <span className="h-2.5 w-2.5 rounded-full bg-brand-600/70" />
          <span className="h-2.5 w-2.5 rounded-full bg-amber-500/40" />
          <span className="h-2.5 w-2.5 rounded-full bg-emerald-500/40" />
          <span className="ml-2 font-mono">{label}</span>
        </div>
      ) : null}
      <pre className="overflow-x-auto px-4 py-4 text-[13px] leading-relaxed text-slate-300">
        <code className="font-mono">{code}</code>
      </pre>
    </div>
  );
}

export function Card({
  icon,
  title,
  children,
  tone = "default",
}: {
  icon: ReactNode;
  title: string;
  children: ReactNode;
  tone?: "default" | "warn" | "good";
}) {
  const tones = {
    default: "border-line bg-ink-900 hover:border-brand-600/50",
    warn: "border-amber-500/25 bg-amber-500/[0.04] hover:border-amber-500/50",
    good: "border-emerald-500/25 bg-emerald-500/[0.04] hover:border-emerald-500/50",
  } as const;

  return (
    <div
      className={`group rounded-2xl border p-5 transition-colors duration-300 ${tones[tone]}`}
    >
      <div className="mb-3 inline-flex h-10 w-10 items-center justify-center rounded-xl bg-ink-800 text-brand-500 ring-1 ring-line transition-transform duration-300 group-hover:scale-105">
        {icon}
      </div>
      <h3 className="text-base font-semibold text-white">{title}</h3>
      <div className="mt-2 text-sm leading-relaxed text-slate-400">{children}</div>
    </div>
  );
}

export function Badge({
  children,
  tone = "brand",
}: {
  children: ReactNode;
  tone?: "brand" | "neutral" | "warn" | "good";
}) {
  const tones = {
    brand: "border-brand-600/40 bg-brand-600/10 text-brand-400",
    neutral: "border-line bg-ink-800 text-slate-400",
    warn: "border-amber-500/30 bg-amber-500/10 text-amber-300",
    good: "border-emerald-500/30 bg-emerald-500/10 text-emerald-300",
  } as const;

  return (
    <span
      className={`inline-flex items-center gap-1.5 rounded-full border px-3 py-1 text-xs font-medium ${tones[tone]}`}
    >
      {children}
    </span>
  );
}

export function Icon({ path, className = "h-5 w-5" }: { path: string; className?: string }) {
  return (
    <svg
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth={1.8}
      strokeLinecap="round"
      strokeLinejoin="round"
      className={className}
    >
      <path d={path} />
    </svg>
  );
}
