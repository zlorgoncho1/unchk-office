package sn.unchk.office.admin;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sn.unchk.office.admin.authz.FournisseurAttributsAdmin;
import sn.unchk.office.admin.domain.Budget;
import sn.unchk.office.admin.repository.BudgetRepository;
import sn.unchk.office.common.authz.EntreeOpa;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Tests du fournisseur d'attributs ABAC (anti-IDOR) : l'enrichissement de la ressource OPA
 * doit refléter le propriétaire réel du budget chargé en base.
 */
@ExtendWith(MockitoExtension.class)
class FournisseurAttributsAdminTest {

    @Mock
    private BudgetRepository budgetRepository;

    @InjectMocks
    private FournisseurAttributsAdmin fournisseur;

    @Test
    void attributs_d_un_budget_exposent_le_proprietaire_et_la_visibilite() {
        // Étant donné un budget possédé par un utilisateur donné
        UUID budgetId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        Budget budget = new Budget();
        budget.setOwnerId(ownerId);
        when(budgetRepository.findById(budgetId)).thenReturn(Optional.of(budget));

        // Quand la garde demande les attributs ABAC
        EntreeOpa.Ressource ressource = fournisseur.attributs("budget", budgetId.toString());

        // Alors la ressource expose le propriétaire et la visibilité par rôle
        assertThat(ressource.type()).isEqualTo("budget");
        assertThat(ressource.ownerId()).isEqualTo(ownerId.toString());
        assertThat(ressource.visibility()).contains("admin", "administratif");
    }

    @Test
    void attributs_d_un_type_inconnu_renvoient_une_ressource_minimale() {
        // Quand on demande un type non géré par le service
        EntreeOpa.Ressource ressource = fournisseur.attributs("inconnu", UUID.randomUUID().toString());

        // Alors aucun propriétaire n'est inféré ; OPA décidera avec ses propres données
        assertThat(ressource.ownerId()).isNull();
        assertThat(ressource.visibility()).isEmpty();
    }
}
