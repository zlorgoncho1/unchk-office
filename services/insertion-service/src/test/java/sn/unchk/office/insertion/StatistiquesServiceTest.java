package sn.unchk.office.insertion;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sn.unchk.office.insertion.domain.InsertionKind;
import sn.unchk.office.insertion.dto.StatistiquesInsertion;
import sn.unchk.office.insertion.projection.AcademicFormationRo;
import sn.unchk.office.insertion.repository.AcademicFormationRoRepository;
import sn.unchk.office.insertion.repository.InsertionOutcomeRepository;
import sn.unchk.office.insertion.service.StatistiquesService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Tests du calcul des statistiques d'insertion (auto-emploi vs emploi salarié).
 * On vérifie l'agrégation globale, l'agrégation par formation et le libellé
 * issu du read-model local (sans appel REST).
 */
@ExtendWith(MockitoExtension.class)
class StatistiquesServiceTest {

    @Mock
    private InsertionOutcomeRepository outcomes;

    @Mock
    private AcademicFormationRoRepository formations;

    @InjectMocks
    private StatistiquesService service;

    @Test
    void doitAgregerLesSituationsParTypeEtParFormation() {
        UUID formationA = UUID.randomUUID();

        // Répartition globale : 3 en emploi salarié, 2 en auto-emploi.
        when(outcomes.compterParTypeCourant()).thenReturn(List.of(
                new Object[]{InsertionKind.emploi_salarie, 3L},
                new Object[]{InsertionKind.auto_emploi, 2L}
        ));

        // Répartition par formation : la formation A a 3 salariés et 1 auto-emploi.
        when(outcomes.compterParFormationEtTypeCourant()).thenReturn(List.of(
                new Object[]{formationA, InsertionKind.emploi_salarie, 3L},
                new Object[]{formationA, InsertionKind.auto_emploi, 1L}
        ));

        // Le libellé de la formation provient du read-model local.
        AcademicFormationRo ro = formationRo(formationA, "Licence Informatique");
        lenient().when(formations.findById(formationA)).thenReturn(Optional.of(ro));

        StatistiquesInsertion stats = service.calculer();

        // Total global = 5 situations courantes.
        assertThat(stats.total()).isEqualTo(5L);
        assertThat(stats.parType().get("emploi_salarie")).isEqualTo(3L);
        assertThat(stats.parType().get("auto_emploi")).isEqualTo(2L);

        // Une seule formation, avec son libellé et son total.
        assertThat(stats.parFormation()).hasSize(1);
        StatistiquesInsertion.StatistiqueFormation detail = stats.parFormation().get(0);
        assertThat(detail.formationLabel()).isEqualTo("Licence Informatique");
        assertThat(detail.total()).isEqualTo(4L);
        assertThat(detail.parType().get("emploi_salarie")).isEqualTo(3L);
        assertThat(detail.parType().get("auto_emploi")).isEqualTo(1L);
    }

    @Test
    void doitRenvoyerUnLibelleParDefautSiFormationNonProjetee() {
        UUID formationInconnue = UUID.randomUUID();

        when(outcomes.compterParTypeCourant()).thenReturn(List.of(
                new Object[]{InsertionKind.recherche_emploi, 1L}
        ));
        when(outcomes.compterParFormationEtTypeCourant()).thenReturn(List.of(
                new Object[]{formationInconnue, InsertionKind.recherche_emploi, 1L}
        ));
        // La formation n'est pas (encore) dans le read-model : libellé « n/a ».
        when(formations.findById(formationInconnue)).thenReturn(Optional.empty());

        StatistiquesInsertion stats = service.calculer();

        assertThat(stats.parFormation()).hasSize(1);
        assertThat(stats.parFormation().get(0).formationLabel()).isEqualTo("n/a");
    }

    /** Fabrique un read-model formation pour les tests. */
    private AcademicFormationRo formationRo(UUID id, String label) {
        AcademicFormationRo ro = new AcademicFormationRo();
        ro.setId(id);
        ro.setLabel(label);
        ro.setLevel("licence");
        ro.setLastEventAt(java.time.OffsetDateTime.now());
        return ro;
    }
}
