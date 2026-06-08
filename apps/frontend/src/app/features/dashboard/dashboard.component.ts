import {
  CUSTOM_ELEMENTS_SCHEMA,
  Component,
  computed,
  inject,
} from '@angular/core';
import { MatButtonModule } from '@angular/material/button';

import { AuthService } from '../../core/auth/auth.service';
import { LIBELLES_ROLES, Role } from '../../core/models';
import {
  StatCardComponent,
  PageHeaderComponent,
  SectionCardComponent,
  EmptyStateComponent,
} from '../../shared/ui';
import { StatCardTon } from '../../shared/ui';

// Un KPI affiché sur le tableau de bord.
interface Kpi {
  libelle: string;
  valeur: string;
  icone: string;
  ton: StatCardTon;
  variation?: string;
  positive?: boolean;
  roles?: Role[]; // rôles qui voient ce KPI (vide = tous)
}

/**
 * Tableau de bord d'accueil.
 * Profil d'accueil dépendant du rôle : message de bienvenue, KPI filtrés et
 * raccourcis. Démontre l'usage des composants UI brandés (StatCard, PageHeader,
 * SectionCard, EmptyState).
 */
@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [
    MatButtonModule,
    StatCardComponent,
    PageHeaderComponent,
    SectionCardComponent,
    EmptyStateComponent,
  ],
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss',
})
export class DashboardComponent {
  private readonly auth = inject(AuthService);

  readonly user = this.auth.currentUser;

  // Rôle principal (libellé) pour personnaliser l'accueil.
  readonly rolePrincipal = computed(() => {
    const roles = this.user()?.roles ?? [];
    return roles.length ? LIBELLES_ROLES[roles[0]] : '';
  });

  // Prénom (ou début d'email) pour le message de bienvenue.
  readonly prenom = computed(() => {
    const u = this.user();
    const source = u?.fullName?.trim() || u?.email || '';
    return source.split(/[\s@.]+/)[0] || 'Bienvenue';
  });

  // KPI de référence ; filtrés selon le rôle de l'utilisateur.
  private readonly tousLesKpis: Kpi[] = [
    {
      libelle: 'Formations actives',
      valeur: '24',
      icone: 'square-academic-cap-bold-duotone',
      ton: 'blue',
      variation: '+3 ce semestre',
      positive: true,
      roles: ['admin', 'administratif', 'enseignant'],
    },
    {
      libelle: 'Étudiants suivis',
      valeur: '1 287',
      icone: 'users-group-rounded-bold-duotone',
      ton: 'green',
      variation: '+5,2%',
      positive: true,
      roles: ['admin', 'administratif', 'enseignant', 'appui-insertion'],
    },
    {
      libelle: 'Réunions à venir',
      valeur: '6',
      icone: 'calendar-bold-duotone',
      ton: 'orange',
      roles: ['admin', 'administratif', 'enseignant', 'appui-insertion', 'etudiant'],
    },
    {
      libelle: 'Taux d’insertion',
      valeur: '78%',
      icone: 'chart-2-bold-duotone',
      ton: 'navy',
      variation: '+2,1%',
      positive: true,
      roles: ['admin', 'administratif', 'appui-insertion'],
    },
    {
      libelle: 'Mes documents',
      valeur: '12',
      icone: 'folder-with-files-bold-duotone',
      ton: 'blue',
      roles: ['etudiant'],
    },
    {
      libelle: 'Budget engagé',
      valeur: '64%',
      icone: 'wallet-money-bold-duotone',
      ton: 'orange',
      roles: ['admin', 'administratif'],
    },
  ];

  // KPI visibles pour l'utilisateur courant (admin voit tout).
  readonly kpis = computed<Kpi[]>(() => {
    const roles = this.user()?.roles ?? [];
    const estAdmin = roles.includes('admin');
    return this.tousLesKpis.filter(
      (k) => estAdmin || !k.roles || k.roles.some((r) => roles.includes(r))
    );
  });
}
