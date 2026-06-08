# Charte graphique — UNCHK Office

Charte dérivée du **logo de l'Université Cheikh Hamidou Kane** (monogramme « UN » en boucles
bleu / vert / orange, texte bleu marine sur fond blanc). Objectif : une interface **sobre, lisible et
réellement marquée UNCHK** — surtout pas un rendu générique « AI generated ».

> Source de vérité des valeurs : [`brand/design-tokens.json`](../brand/design-tokens.json).
> Variables prêtes à l'emploi : [`brand/tokens.css`](../brand/tokens.css) et [`brand/_tokens.scss`](../brand/_tokens.scss).

## Palette

| Rôle | Couleur | Hex | Usage |
|---|---|---|---|
| Primaire | Bleu UNCHK | `#1C75BC` | Navigation, liens, états actifs, boutons principaux |
| Secondaire | Vert UNCHK | `#36A93B` | Succès, statistiques positives, actions secondaires |
| Accent | Orange UNCHK | `#F39200` | Appels à l'action, badges, mises en avant ponctuelles |
| Texte | Bleu marine | `#16314A` | Titres et libellés (fort contraste sur blanc) |
| Texte 2 | Bleu ardoise | `#3E6E8E` | Sous-titres, métadonnées |
| Fond | Blanc / gris très clair | `#FFFFFF` / `#F4F7FA` | Fond d'application et surfaces |

Sémantique : succès `#36A93B`, avertissement `#F39200`, erreur `#E5484D`, info `#1C75BC`.

## Typographie

- Police d'interface : **Inter** (repli `Segoe UI`, system-ui).
- Échelle : display 32, h1 26, h2 21, h3 17, corps 14, petit 13, légende 12 (px).

## Iconographie

- **Iconify** avec le set **Solar** (`@iconify-json/solar`), via le web component `<iconify-icon>`.
- Style par défaut : `bold-duotone` ; `bold` pour les éléments actifs.
- Exemples : tableau de bord `solar:widget-5-bold-duotone`, documents `solar:documents-bold-duotone`,
  étudiants `solar:users-group-rounded-bold-duotone`, formations `solar:square-academic-cap-bold-duotone`,
  notifications `solar:bell-bing-bold-duotone`.

## Logo

- `brand/logos/unchk-horizontal.png` → barre supérieure et en-têtes larges.
- `brand/logos/unchk-vertical.png` → écran de connexion / splash.
- Toujours sur **fond blanc**, jamais sur fond chargé. Marge de protection = hauteur du monogramme.

## Structure d'écran de référence

Inspirée d'un tableau de bord d'administration moderne, **adaptée en thème clair** :

- **Barre latérale gauche** : logo en haut, sections groupées (« TABLEAUX DE BORD », « GESTION »,
  « PARAMÈTRES »), élément actif en pilule colorée (bleu primaire).
- **Barre supérieure** : fil d'Ariane, recherche, icônes (thème, rafraîchir, notifications, langue), avatar.
- **Contenu** : rangée de cartes KPI, graphiques (anneau + aires), tableaux triables.
- **Rail droit (optionnel)** : notifications, activités récentes, contacts.

## Règles « anti-générique »

- Utiliser les **vraies couleurs de marque** ci-dessus, pas de dégradés violets/néon par défaut.
- Icônes **Solar** cohérentes partout (pas de mélange de sets).
- Ombres **discrètes**, grille **8 px**, libellés en **français** réels (pas de lorem ipsum en production).
- Réutiliser le motif du **monogramme** UNCHK avec parcimonie comme signature visuelle.
