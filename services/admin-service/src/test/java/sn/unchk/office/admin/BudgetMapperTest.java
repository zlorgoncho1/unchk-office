package sn.unchk.office.admin;

import org.junit.jupiter.api.Test;
import sn.unchk.office.admin.domain.Budget;
import sn.unchk.office.admin.domain.BudgetLine;
import sn.unchk.office.admin.domain.BudgetLineDirection;
import sn.unchk.office.admin.dto.BudgetDto;
import sn.unchk.office.admin.dto.LigneBudgetaireDto;
import sn.unchk.office.admin.mapper.BudgetMapper;
import sn.unchk.office.admin.messaging.BudgetEventPayload;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests du mapper budgétaire : calcul des écarts prévu/réalisé et construction de la charge utile.
 */
class BudgetMapperTest {

    private final BudgetMapper mapper = new BudgetMapper();

    @Test
    void versDto_calcule_l_ecart_global_et_celui_de_chaque_ligne() {
        // Étant donné un budget avec un total prévu de 1000 et réalisé de 600
        Budget budget = budgetExemple(new BigDecimal("1000.00"), new BigDecimal("600.00"));
        BudgetLine ligne = ligneExemple(budget.getId(),
                new BigDecimal("1000.00"), new BigDecimal("600.00"));

        // Quand on construit le DTO détaillé
        BudgetDto dto = mapper.versDto(budget, List.of(ligne));

        // Alors l'écart global vaut prévu − réalisé = 400
        assertThat(dto.ecartGlobal()).isEqualByComparingTo("400.00");
        assertThat(dto.lignes()).hasSize(1);
        LigneBudgetaireDto ligneDto = dto.lignes().get(0);
        assertThat(ligneDto.ecart()).isEqualByComparingTo("400.00");
    }

    @Test
    void versPayload_reprend_les_champs_d_etat_du_budget() {
        // Étant donné un budget existant
        Budget budget = budgetExemple(new BigDecimal("500.00"), new BigDecimal("250.00"));

        // Quand on produit la charge utile destinée à Kafka
        BudgetEventPayload payload = mapper.versPayload(budget);

        // Alors elle reflète l'état de l'agrégat
        assertThat(payload.budgetId()).isEqualTo(budget.getId());
        assertThat(payload.fiscalYear()).isEqualTo(budget.getFiscalYear());
        assertThat(payload.totalPlanned()).isEqualByComparingTo("500.00");
        assertThat(payload.totalRealized()).isEqualByComparingTo("250.00");
        assertThat(payload.currency()).isEqualTo("XOF");
    }

    /** Construit un budget de test avec un identifiant et des totaux donnés. */
    private Budget budgetExemple(BigDecimal prevu, BigDecimal realise) {
        Budget budget = new Budget();
        budget.setFiscalYear((short) 2026);
        budget.setLabel("Budget de fonctionnement");
        budget.setTotalPlanned(prevu);
        budget.setTotalRealized(realise);
        budget.setCurrency("XOF");
        // L'identifiant est normalement posé par @PrePersist : on le force pour le test.
        forcerId(budget);
        return budget;
    }

    /** Construit une ligne de test. */
    private BudgetLine ligneExemple(UUID budgetId, BigDecimal prevu, BigDecimal realise) {
        BudgetLine ligne = new BudgetLine();
        ligne.setBudgetId(budgetId);
        ligne.setCategory("Salaires");
        ligne.setDirection(BudgetLineDirection.depense);
        ligne.setPlannedAmount(prevu);
        ligne.setRealizedAmount(realise);
        return ligne;
    }

    /** Force un identifiant via réflexion (l'id est normalement généré à la persistance). */
    private void forcerId(Budget budget) {
        try {
            var champ = Budget.class.getDeclaredField("id");
            champ.setAccessible(true);
            champ.set(budget, UUID.randomUUID());
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
