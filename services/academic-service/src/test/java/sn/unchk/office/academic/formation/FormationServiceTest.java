package sn.unchk.office.academic.formation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sn.unchk.office.academic.formation.dto.FormationCreationDto;
import sn.unchk.office.academic.formation.event.FormationEventPublisher;
import sn.unchk.office.common.audit.AuditLogger;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests unitaires de la logique métier des formations.
 * On vérifie la création, les règles de cohérence et l'émission de l'événement Kafka.
 */
@ExtendWith(MockitoExtension.class)
class FormationServiceTest {

    @Mock
    private FormationRepository formationRepository;
    @Mock
    private FormationEventPublisher eventPublisher;
    @Mock
    private AuditLogger auditLogger;

    @InjectMocks
    private FormationService formationService;

    private UUID createur;

    @BeforeEach
    void preparer() {
        createur = UUID.randomUUID();
    }

    @Test
    void creation_persiste_et_publie_evenement() {
        // Étant donné des données de formation valides...
        FormationCreationDto dto = new FormationCreationDto(
                "LIC-INFO", "Licence Informatique", NiveauFormation.LICENCE, TypeFormation.INITIALE,
                Financement.ETAT, new java.math.BigDecimal("1500000"),
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31),
                30, 20, null);
        when(formationRepository.existsByCode("LIC-INFO")).thenReturn(false);
        // Le dépôt renvoie l'entité qu'on lui passe en lui affectant un ID (simule la persistance).
        when(formationRepository.save(any(Formation.class))).thenAnswer(inv -> {
            Formation f = inv.getArgument(0);
            if (f.getId() == null) f.setId(UUID.randomUUID());
            return f;
        });

        // Quand on crée la formation...
        Formation creee = formationService.creer(dto, createur);

        // Alors les champs sont repris, l'auteur est positionné et l'événement Created est publié.
        assertThat(creee.getLabel()).isEqualTo("Licence Informatique");
        assertThat(creee.getLevel()).isEqualTo(NiveauFormation.LICENCE);
        assertThat(creee.getTrainedMale()).isEqualTo(30);
        assertThat(creee.getCreatedBy()).isEqualTo(createur);

        ArgumentCaptor<Formation> capteur = ArgumentCaptor.forClass(Formation.class);
        verify(eventPublisher).publierCreation(capteur.capture());
        assertThat(capteur.getValue().getCode()).isEqualTo("LIC-INFO");
    }

    @Test
    void creation_refuse_un_code_deja_utilise() {
        // Étant donné un code déjà présent en base...
        FormationCreationDto dto = new FormationCreationDto(
                "DUP", "Doublon", NiveauFormation.MASTER, null, null, null, null, null, 0, 0, null);
        when(formationRepository.existsByCode("DUP")).thenReturn(true);

        // Quand on tente la création, alors un conflit est levé et rien n'est publié.
        assertThatThrownBy(() -> formationService.creer(dto, createur))
                .isInstanceOf(CodeFormationDejaUtiliseException.class);
        verify(eventPublisher, never()).publierCreation(any());
    }

    @Test
    void creation_refuse_une_periode_incoherente() {
        // Étant donné une date de fin antérieure à la date de début...
        FormationCreationDto dto = new FormationCreationDto(
                null, "Formation", NiveauFormation.CERTIFICAT, null, null, null,
                LocalDate.of(2025, 6, 1), LocalDate.of(2025, 1, 1), 0, 0, null);

        // Quand on tente la création, alors la règle de période rejette la demande.
        assertThatThrownBy(() -> formationService.creer(dto, createur))
                .isInstanceOf(IllegalArgumentException.class);
        verify(formationRepository, never()).save(any());
    }

    @Test
    void obtenir_inexistante_leve_404() {
        // Étant donné un identifiant inconnu (ou supprimé)...
        UUID id = UUID.randomUUID();
        when(formationRepository.findByIdAndDeletedAtIsNull(id)).thenReturn(Optional.empty());

        // Quand on demande la formation, alors l'exception "introuvable" (404) est levée.
        assertThatThrownBy(() -> formationService.obtenir(id))
                .isInstanceOf(FormationIntrouvableException.class);
    }

    @Test
    void suppression_marque_logiquement_et_publie_tombstone() {
        // Étant donné une formation existante...
        UUID id = UUID.randomUUID();
        Formation formation = new Formation();
        formation.setId(id);
        formation.setLabel("À supprimer");
        formation.setLevel(NiveauFormation.LICENCE);
        formation.setCreatedBy(createur);
        when(formationRepository.findByIdAndDeletedAtIsNull(id)).thenReturn(Optional.of(formation));
        when(formationRepository.save(any(Formation.class))).thenAnswer(inv -> inv.getArgument(0));

        // Quand on la supprime...
        formationService.supprimer(id, createur);

        // Alors elle est marquée supprimée logiquement et l'événement Deleted est publié.
        assertThat(formation.estSupprimee()).isTrue();
        assertThat(formation.isActive()).isFalse();
        verify(eventPublisher).publierSuppression(id, createur);
    }
}
