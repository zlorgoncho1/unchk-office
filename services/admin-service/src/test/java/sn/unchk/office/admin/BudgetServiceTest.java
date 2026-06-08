package sn.unchk.office.admin;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sn.unchk.office.admin.domain.Budget;
import sn.unchk.office.admin.domain.BudgetLine;
import sn.unchk.office.admin.domain.BudgetLineDirection;
import sn.unchk.office.admin.dto.BudgetDto;
import sn.unchk.office.admin.dto.CreationBudgetDto;
import sn.unchk.office.admin.dto.CreationLigneBudgetaireDto;
import sn.unchk.office.admin.mapper.BudgetMapper;
import sn.unchk.office.admin.messaging.BudgetEventProducer;
import sn.unchk.office.admin.repository.BudgetLineRepository;
import sn.unchk.office.admin.repository.BudgetRepository;
import sn.unchk.office.admin.service.BudgetService;
import sn.unchk.office.admin.service.ConflitRessourceException;
import sn.unchk.office.common.audit.AuditLogger;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests du service budgétaire : unicité à la création, recalcul des totaux, émission d'événements.
 * Les dépendances (repositories, producteur Kafka, audit) sont simulées par Mockito.
 */
@ExtendWith(MockitoExtension.class)
class BudgetServiceTest {

    @Mock
    private BudgetRepository budgetRepository;
    @Mock
    private BudgetLineRepository budgetLineRepository;
    @Mock
    private BudgetEventProducer producteur;
    @Mock
    private AuditLogger audit;

    private BudgetService service;

    @BeforeEach
    void initialiser() {
        // On utilise le vrai mapper (pas de comportement à simuler) et les mocks pour le reste.
        service = new BudgetService(budgetRepository, budgetLineRepository,
                new BudgetMapper(), producteur, audit);
    }

    @Test
    void creer_refuse_un_budget_en_doublon_exercice_libelle() {
        // Étant donné un couple (exercice, libellé) déjà présent
        CreationBudgetDto dto = new CreationBudgetDto((short) 2026, "Budget principal", null, "XOF");
        when(budgetRepository.existsByFiscalYearAndLabel((short) 2026, "Budget principal"))
                .thenReturn(true);

        // Quand / Alors : la création lève un conflit et ne publie aucun événement
        assertThatThrownBy(() -> service.creer(dto))
                .isInstanceOf(ConflitRessourceException.class);
        verify(producteur, never()).publier(any(), any());
    }

    @Test
    void creer_persiste_publie_un_evenement_et_renvoie_le_dto() {
        // Étant donné un budget inédit
        CreationBudgetDto dto = new CreationBudgetDto((short) 2026, "Investissement", "Note", "EUR");
        when(budgetRepository.existsByFiscalYearAndLabel(any(), any())).thenReturn(false);
        when(budgetRepository.save(any(Budget.class))).thenAnswer(invocation -> {
            Budget b = invocation.getArgument(0);
            forcerId(b);
            return b;
        });
        when(budgetLineRepository.findByBudgetIdOrderByCategoryAsc(any())).thenReturn(List.of());

        // Quand on crée le budget
        BudgetDto resultat = service.creer(dto);

        // Alors le DTO reflète l'entrée et un événement "BudgetCree" est publié
        assertThat(resultat.label()).isEqualTo("Investissement");
        assertThat(resultat.orientationNote()).isEqualTo("Note");
        assertThat(resultat.currency()).isEqualTo("EUR");
        ArgumentCaptor<String> typeCaptor = ArgumentCaptor.forClass(String.class);
        verify(producteur).publier(typeCaptor.capture(), any());
        assertThat(typeCaptor.getValue()).isEqualTo("BudgetCree");
    }

    @Test
    void ajouterLigne_recalcule_les_totaux_a_partir_des_lignes() {
        // Étant donné un budget existant et une ligne à ajouter
        UUID budgetId = UUID.randomUUID();
        Budget budget = new Budget();
        forcerId(budget, budgetId);
        when(budgetRepository.findById(budgetId)).thenReturn(Optional.of(budget));
        when(budgetRepository.save(any(Budget.class))).thenAnswer(i -> i.getArgument(0));

        BudgetLine ligne = new BudgetLine();
        ligne.setBudgetId(budgetId);
        ligne.setCategory("Fournitures");
        ligne.setDirection(BudgetLineDirection.depense);
        ligne.setPlannedAmount(new BigDecimal("300.00"));
        ligne.setRealizedAmount(new BigDecimal("120.00"));
        // Après ajout, le service relit les lignes pour recalculer les totaux.
        when(budgetLineRepository.findByBudgetIdOrderByCategoryAsc(budgetId))
                .thenReturn(List.of(ligne));

        CreationLigneBudgetaireDto dto = new CreationLigneBudgetaireDto(
                "Fournitures", BudgetLineDirection.depense, new BigDecimal("300.00"), null);

        // Quand on ajoute la ligne
        BudgetDto resultat = service.ajouterLigne(budgetId, dto);

        // Alors les totaux du budget sont recalculés (prévu 300, réalisé 120)
        assertThat(resultat.totalPlanned()).isEqualByComparingTo("300.00");
        assertThat(resultat.totalRealized()).isEqualByComparingTo("120.00");
        assertThat(resultat.ecartGlobal()).isEqualByComparingTo("180.00");
        verify(producteur).publier(any(), any());
    }

    /** Force un identifiant aléatoire sur un budget de test. */
    private void forcerId(Budget budget) {
        forcerId(budget, UUID.randomUUID());
    }

    /** Force un identifiant donné via réflexion. */
    private void forcerId(Budget budget, UUID id) {
        try {
            var champ = Budget.class.getDeclaredField("id");
            champ.setAccessible(true);
            champ.set(budget, id);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
