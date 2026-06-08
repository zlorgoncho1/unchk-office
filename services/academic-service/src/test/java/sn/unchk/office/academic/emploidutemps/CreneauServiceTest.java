package sn.unchk.office.academic.emploidutemps;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sn.unchk.office.academic.emploidutemps.dto.CreneauCreationDto;
import sn.unchk.office.academic.formation.Formation;
import sn.unchk.office.academic.formation.FormationService;
import sn.unchk.office.common.audit.AuditLogger;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests unitaires des règles de cohérence des créneaux d'emploi du temps.
 */
@ExtendWith(MockitoExtension.class)
class CreneauServiceTest {

    @Mock
    private CreneauRepository creneauRepository;
    @Mock
    private FormationService formationService;
    @Mock
    private AuditLogger auditLogger;

    @InjectMocks
    private CreneauService creneauService;

    private final UUID formationId = UUID.randomUUID();

    @Test
    void ajoute_un_creneau_recurrent_valide() {
        // Étant donné une formation existante et un créneau récurrent cohérent...
        when(formationService.obtenir(formationId)).thenReturn(new Formation());
        when(creneauRepository.save(any(Creneau.class))).thenAnswer(inv -> inv.getArgument(0));
        CreneauCreationDto dto = new CreneauCreationDto(
                "Algorithmique", UUID.randomUUID(), JourSemaine.LUNDI, null,
                LocalTime.of(8, 0), LocalTime.of(10, 0), "Salle 1");

        // Quand on ajoute le créneau, alors il est persisté sans erreur.
        creneauService.ajouter(formationId, dto);
        verify(creneauRepository).save(any(Creneau.class));
    }

    @Test
    void refuse_un_creneau_a_la_fois_recurrent_et_ponctuel() {
        // Étant donné un créneau qui fixe jour ET date (viole le CHECK XOR)...
        lenient().when(formationService.obtenir(formationId)).thenReturn(new Formation());
        CreneauCreationDto dto = new CreneauCreationDto(
                "Cours", null, JourSemaine.MARDI, LocalDate.now(),
                LocalTime.of(8, 0), LocalTime.of(9, 0), null);

        // Quand on l'ajoute, alors la règle de récurrence exclusive rejette la demande.
        assertThatThrownBy(() -> creneauService.ajouter(formationId, dto))
                .isInstanceOf(IllegalArgumentException.class);
        verify(creneauRepository, never()).save(any());
    }

    @Test
    void refuse_des_horaires_incoherents() {
        // Étant donné un créneau dont la fin précède le début...
        lenient().when(formationService.obtenir(formationId)).thenReturn(new Formation());
        CreneauCreationDto dto = new CreneauCreationDto(
                "Cours", null, JourSemaine.MERCREDI, null,
                LocalTime.of(10, 0), LocalTime.of(9, 0), null);

        // Quand on l'ajoute, alors la règle d'horaires rejette la demande.
        assertThatThrownBy(() -> creneauService.ajouter(formationId, dto))
                .isInstanceOf(IllegalArgumentException.class);
        verify(creneauRepository, never()).save(any());
    }
}
