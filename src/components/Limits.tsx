import { Card, Icon, Section } from "./ui";

const imposed = [
  {
    title: "Publicité de YouTube",
    body: "Le lecteur officiel peut diffuser ses propres publicités. L’application n’en ajoute pas et ne peut pas (ni ne doit) supprimer celles de YouTube.",
  },
  {
    title: "Lecture en arrière-plan",
    body: "La lecture hors application dépend du lecteur officiel et des OEM : elle n’est pas garantie. Aucun service de fond, aucune extraction de flux.",
  },
  {
    title: "Vidéos non intégrables",
    body: "Vidéos privées, réservées aux membres, à restriction d’âge ou dont le propriétaire a désactivé l’embed : l’application affiche « non disponible » et propose l’ouverture dans YouTube.",
  },
  {
    title: "Restrictions géographiques",
    body: "Une vidéo bloquée dans le pays de l’utilisateur reste bloquée : c’est une décision de YouTube, non contournable.",
  },
  {
    title: "Quota de l’API",
    body: "10 000 unités par jour par défaut, 100 unités par recherche. L’application limite les appels (cache mémoire + cache HTTP) mais ne supprime pas la limite.",
  },
  {
    title: "Pas de téléchargement",
    body: "Aucun export audio/vidéo, aucun contournement de la publicité, aucune redistribution. L’application reste un lecteur des contenus que YouTube diffuse.",
  },
];

const ours = [
  "Aucune publicité ajoutée par l’application",
  "Aucun abonnement, aucun compte, aucun paiement",
  "Aucun traçage, aucune télémétrie, aucun partage de données",
  "Favoris et historique conservés uniquement sur l’appareil",
  "Clé API chiffrée ou confinée au serveur via un proxy",
  "Code source complet, buildable et auditable",
];

export default function Limits() {
  return (
    <Section
      id="limites"
      eyebrow="Honnêteté technique"
      title="Ce que l’application fait — et ce que YouTube impose"
      description="Le lecteur intégré est celui de YouTube. L’application ne prétend ni supprimer les publicités, ni garantir la lecture en arrière-plan, ni contourner les règles de diffusion."
    >
      <div className="grid gap-6 lg:grid-cols-2">
        <div className="rounded-2xl border border-emerald-500/25 bg-emerald-500/[0.04] p-6">
          <h3 className="flex items-center gap-2 text-lg font-semibold text-white">
            <span className="flex h-8 w-8 items-center justify-center rounded-lg bg-emerald-500/15 text-emerald-300">
              <Icon path="M20 6 9 17l-5-5" />
            </span>
            Garanties de l’application
          </h3>
          <ul className="mt-5 space-y-3">
            {ours.map((item) => (
              <li key={item} className="flex items-start gap-3 text-sm text-slate-300">
                <span className="mt-1.5 h-1.5 w-1.5 shrink-0 rounded-full bg-emerald-400" />
                {item}
              </li>
            ))}
          </ul>
        </div>

        <div className="rounded-2xl border border-amber-500/25 bg-amber-500/[0.04] p-6">
          <h3 className="flex items-center gap-2 text-lg font-semibold text-white">
            <span className="flex h-8 w-8 items-center justify-center rounded-lg bg-amber-500/15 text-amber-300">
              <Icon path="M12 9v4m0 4h.01M10.3 3.9 1.8 18a2 2 0 0 0 1.7 3h17a2 2 0 0 0 1.7-3L13.7 3.9a2 2 0 0 0-3.4 0z" />
            </span>
            Limites imposées par YouTube
          </h3>
          <ul className="mt-5 space-y-3">
            {imposed.map((item) => (
              <li key={item.title} className="text-sm text-slate-300">
                <span className="font-medium text-white">{item.title}.</span> {item.body}
              </li>
            ))}
          </ul>
        </div>
      </div>

      <div className="mt-6 grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
        <Card
          tone="warn"
          icon={<Icon path="M4 16v-4m4 8v-8m4 12V8m4 10V4m4 14v-6" />}
          title="Zéro extraction de flux"
        >
          Aucune capture du flux réseau, aucun contournement du lecteur, aucun téléchargement :
          l’application ne fait qu’afficher ce que YouTube sert.
        </Card>
        <Card
          tone="warn"
          icon={<Icon path="M12 2 4 6v6c0 5 3.4 9.4 8 10 4.6-.6 8-5 8-10V6z" />}
          title="Conditions d’utilisation"
        >
          L’usage de la Data API v3 est encadré : restrictions de clé, quotas, absence de
          redistribution et respect des règles de contenu YouTube.
        </Card>
        <Card
          tone="warn"
          icon={<Icon path="M12 22a10 10 0 1 0 0-20 10 10 0 0 0 0 20zM12 8v5m0 3h.01" />}
          title="Transparence"
        >
          Si une fonctionnalité n’est pas prise en charge officiellement par YouTube, l’application
          l’indique au lieu de la simuler.
        </Card>
      </div>
    </Section>
  );
}
