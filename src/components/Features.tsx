import { Card, Icon, Section } from "./ui";

const features = [
  {
    icon: (
      <Icon path="M21 21l-4.35-4.35M11 19a8 8 0 1 1 0-16 8 8 0 0 1 0 16z" />
    ),
    title: "Recherche API v3",
    body: "Vidéos et chaînes, filtres Tout / Vidéos / Chaînes, debounce 400 ms, pagination automatique et recherches récentes.",
  },
  {
    icon: <Icon path="M4 19V5m0 14h16M8 15l3-4 3 2 4-6" />,
    title: "Tendances par catégorie",
    body: "Vidéos populaires filtrables : Musique, Jeux, Films, Actualités, Sports, Apprentissage.",
  },
  {
    icon: <Icon path="M10 8l6 4-6 4V8zM3 5h18v14H3z" />,
    title: "Lecteur officiel YouTube",
    body: "Lecture via la page /embed gérée par YouTube et pilotée par l’IFrame Player API (play, pause, état).",
  },
  {
    icon: <Icon path="M2 6h20v12H2zM13 11v4h4v-4z" />,
    title: "Picture-in-Picture",
    body: "Bouton dédié et bascule automatique à la sortie de l’application si la vidéo est en cours (API 26+).",
  },
  {
    icon: <Icon path="M12 21s-7-4.35-7-10a4 4 0 0 1 7-2.65A4 4 0 0 1 19 11c0 5.65-7 10-7 10z" />,
    title: "Favoris & historique locaux",
    body: "Base Room sur l’appareil : favoris synchronisés avec l’écran de lecture, historique horodaté et purgable.",
  },
  {
    icon: <Icon path="M20 14.5A8.5 8.5 0 0 1 9.5 4 8.5 8.5 0 1 0 20 14.5z" />,
    title: "Mode sombre Material 3",
    body: "Thème clair, sombre ou système appliqué via AppCompatDelegate et les ressources values-night.",
  },
  {
    icon: <Icon path="M12 9v4m0 4h.01M10.3 3.9 1.8 18a2 2 0 0 0 1.7 3h17a2 2 0 0 0 1.7-3L13.7 3.9a2 2 0 0 0-3.4 0z" />,
    title: "Quota & erreurs gérés",
    body: "Cache mémoire 5 min + cache HTTP 10 Mo, quota épuisé, clé invalide, 429 et hors ligne traduits en messages clairs.",
  },
  {
    icon: <Icon path="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" />,
    title: "Confidentialité",
    body: "Aucune télémétrie, aucun compte, aucune sauvegarde cloud. La clé API est chiffrée par le keystore Android.",
  },
];

export default function Features() {
  return (
    <Section
      id="fonctionnalites"
      eyebrow="Fonctionnalités"
      title="Une application complète, sans contrepartie"
      description="Tout ce qui est nécessaire pour Naviguer dans YouTube au quotidien : recherche, lecture, bibliothèque locale et gestion fine des erreurs réseau."
    >
      <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-4">
        {features.map((feature) => (
          <Card key={feature.title} icon={feature.icon} title={feature.title}>
            {feature.body}
          </Card>
        ))}
      </div>
    </Section>
  );
}
