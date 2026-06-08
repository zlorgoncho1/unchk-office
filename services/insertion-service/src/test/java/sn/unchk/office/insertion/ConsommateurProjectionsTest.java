package sn.unchk.office.insertion;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sn.unchk.office.common.messaging.DomainEvent;
import sn.unchk.office.insertion.messaging.ConsommateurProjections;
import sn.unchk.office.insertion.projection.PeopleStudentRo;
import sn.unchk.office.insertion.repository.AcademicFormationRoRepository;
import sn.unchk.office.insertion.repository.PeopleStudentRoRepository;
import sn.unchk.office.insertion.repository.ProcessedEventRepository;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests du consommateur de projections (read-models locaux alimentés par Kafka).
 * On vérifie la création de la projection étudiant, l'idempotence (dédoublonnage sur
 * eventId) et la suppression logique.
 */
@ExtendWith(MockitoExtension.class)
class ConsommateurProjectionsTest {

    @Mock
    private PeopleStudentRoRepository etudiants;

    @Mock
    private AcademicFormationRoRepository formations;

    @Mock
    private ProcessedEventRepository evenementsTraites;

    @InjectMocks
    private ConsommateurProjections consommateur;

    @Captor
    private ArgumentCaptor<PeopleStudentRo> captureEtudiant;

    @Test
    void doitProjeterUnEtudiantDepuisPeopleStudents() {
        UUID idEtudiant = UUID.randomUUID();
        UUID formationRef = UUID.randomUUID();

        // Payload tel que produit par people-service (clés métier).
        Map<String, Object> payload = Map.of(
                "id", idEtudiant.toString(),
                "fullName", "Awa Diop",
                "gender", "F",
                "formationRef", formationRef.toString(),
                "promotion", "2023-2024");
        DomainEvent<Map<String, Object>> evenement =
                DomainEvent.creer("Created", "trace-1", payload);

        when(etudiants.findById(idEtudiant)).thenReturn(Optional.empty());

        consommateur.surEtudiant(evenement, 12L);

        // La projection est enregistrée avec les bons champs.
        verify(etudiants).save(captureEtudiant.capture());
        PeopleStudentRo ro = captureEtudiant.getValue();
        assertThat(ro.getId()).isEqualTo(idEtudiant);
        assertThat(ro.getFullName()).isEqualTo("Awa Diop");
        assertThat(ro.getGender()).isEqualTo("F");
        assertThat(ro.getFormationRef()).isEqualTo(formationRef);
        assertThat(ro.getEventOffset()).isEqualTo(12L);

        // L'événement est marqué comme traité (idempotence).
        verify(evenementsTraites).save(any());
    }

    @Test
    void doitIgnorerUnEvenementDejaTraite() {
        UUID idEtudiant = UUID.randomUUID();
        Map<String, Object> payload = Map.of("id", idEtudiant.toString(), "gender", "M");
        DomainEvent<Map<String, Object>> evenement =
                DomainEvent.creer("Created", "trace-2", payload);

        // L'eventId est déjà présent dans le journal d'idempotence.
        when(evenementsTraites.existsById(evenement.eventId())).thenReturn(true);

        consommateur.surEtudiant(evenement, 1L);

        // Aucune écriture dans la projection ni nouveau marquage.
        verify(etudiants, never()).save(any());
        verify(evenementsTraites, never()).save(any());
    }

    @Test
    void doitRetirerLaProjectionSurEvenementDeSuppression() {
        UUID idEtudiant = UUID.randomUUID();
        Map<String, Object> payload = Map.of(
                "id", idEtudiant.toString(),
                "deletedAt", "2026-06-08T10:00:00Z");
        DomainEvent<Map<String, Object>> evenement =
                DomainEvent.creer("Deleted", "trace-3", payload);

        consommateur.surEtudiant(evenement, 5L);

        // La projection est supprimée, pas mise à jour.
        verify(etudiants).deleteById(idEtudiant);
        verify(etudiants, never()).save(any());
    }
}
