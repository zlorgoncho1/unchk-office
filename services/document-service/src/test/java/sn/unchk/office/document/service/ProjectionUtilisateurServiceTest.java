package sn.unchk.office.document.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sn.unchk.office.common.messaging.DomainEvent;
import sn.unchk.office.document.domain.IdentityUserRo;
import sn.unchk.office.document.domain.ProcessedEvent;
import sn.unchk.office.document.repository.IdentityUserRoRepository;
import sn.unchk.office.document.repository.ProcessedEventRepository;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests de la projection du read-model des utilisateurs (idempotence + tombstone).
 */
@ExtendWith(MockitoExtension.class)
class ProjectionUtilisateurServiceTest {

    @Mock
    private IdentityUserRoRepository utilisateurs;

    @Mock
    private ProcessedEventRepository evenementsTraites;

    @InjectMocks
    private ProjectionUtilisateurService projection;

    @Test
    void unEvenementCreeUpsertLaProjectionEtMarqueTraite() {
        // Étant donné un événement de création de compte non encore traité...
        UUID userId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        when(evenementsTraites.existsById(eventId)).thenReturn(false);

        DomainEvent<Map<String, Object>> evenement = new DomainEvent<>(
                eventId, "UserCreated", Instant.now(), "trace-1",
                Map.of("id", userId.toString(), "roles", List.of("administratif"), "status", "ACTIVE"));

        // Quand on l'applique...
        projection.appliquer(evenement);

        // Alors la projection est sauvegardée et l'eventId est marqué traité.
        ArgumentCaptor<IdentityUserRo> capteur = ArgumentCaptor.forClass(IdentityUserRo.class);
        verify(utilisateurs).save(capteur.capture());
        assertThat(capteur.getValue().getId()).isEqualTo(userId);
        assertThat(capteur.getValue().getRoles()).contains("administratif");
        verify(evenementsTraites).save(any(ProcessedEvent.class));
    }

    @Test
    void unEvenementDejaTraiteEstIgnore() {
        // Étant donné un eventId déjà présent dans processed_events...
        UUID eventId = UUID.randomUUID();
        when(evenementsTraites.existsById(eventId)).thenReturn(true);

        DomainEvent<Map<String, Object>> evenement = new DomainEvent<>(
                eventId, "UserUpdated", Instant.now(), "trace-2",
                Map.of("id", UUID.randomUUID().toString()));

        // Quand on l'applique...
        projection.appliquer(evenement);

        // Alors rien n'est modifié (idempotence : protection contre le rejeu Kafka).
        verify(utilisateurs, never()).save(any());
        verify(evenementsTraites, never()).save(any());
    }

    @Test
    void unEvenementDeletedSupprimeLEntreeDeProjection() {
        // Étant donné un événement de suppression de compte...
        UUID userId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        when(evenementsTraites.existsById(eventId)).thenReturn(false);

        DomainEvent<Map<String, Object>> evenement = new DomainEvent<>(
                eventId, "UserDeleted", Instant.now(), "trace-3",
                Map.of("id", userId.toString()));

        // Quand on l'applique...
        projection.appliquer(evenement);

        // Alors l'entrée de projection est supprimée.
        verify(utilisateurs, times(1)).deleteById(userId);
        verify(utilisateurs, never()).save(any());
    }

    @Test
    void laConversionDesRolesEnListeFonctionne() {
        // Une chaîne « r1,r2 » doit produire une liste propre.
        assertThat(ProjectionUtilisateurService.rolesEnListe("admin, enseignant"))
                .containsExactly("admin", "enseignant");
        assertThat(ProjectionUtilisateurService.rolesEnListe(null)).isEmpty();
    }
}
