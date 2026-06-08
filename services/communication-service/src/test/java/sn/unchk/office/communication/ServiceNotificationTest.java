package sn.unchk.office.communication;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sn.unchk.office.common.messaging.Topics;
import sn.unchk.office.communication.messaging.producer.EnregistreurEvenement;
import sn.unchk.office.communication.projection.IdentityUserRo;
import sn.unchk.office.communication.repository.IdentityUserRoRepository;
import sn.unchk.office.communication.service.ServiceNotification;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests unitaires de la résolution des destinataires.
 * Vérifie que la diffusion par rôle s'appuie sur le read-model local (zéro REST) et qu'une
 * notification est mise en file par destinataire.
 */
@ExtendWith(MockitoExtension.class)
class ServiceNotificationTest {

    @Mock
    private IdentityUserRoRepository utilisateursRo;
    @Mock
    private EnregistreurEvenement enregistreur;

    @InjectMocks
    private ServiceNotification service;

    @Test
    void notifierParRoles_emet_une_notification_par_destinataire_resolu_localement() {
        // Étant donné deux utilisateurs actifs correspondant aux rôles de visibilité
        IdentityUserRo u1 = new IdentityUserRo(UUID.randomUUID());
        IdentityUserRo u2 = new IdentityUserRo(UUID.randomUUID());
        UUID cible = UUID.randomUUID();
        when(utilisateursRo.trouverActifsParRoles(any(String[].class)))
                .thenReturn(List.of(u1, u2));

        // Quand on notifie par rôles
        service.notifierParRoles(List.of("enseignant"), "compte_rendu", "Titre",
                "Message", "communication", cible);

        // Alors une notification est mise en file pour chacun (clé = recipientId), sur le topic notifications
        verify(enregistreur, times(2)).enregistrer(eq("Notification"), any(UUID.class),
                eq(Topics.NOTIFICATIONS), eq("NotificationCreee"), any());
    }

    @Test
    void notifierParRoles_sans_role_ne_notifie_personne() {
        // Quand la visibilité est vide, aucun destinataire n'est résolu ni notifié
        service.notifierParRoles(List.of(), "compte_rendu", "Titre", "Message",
                "communication", UUID.randomUUID());

        verify(utilisateursRo, never()).trouverActifsParRoles(any());
        verify(enregistreur, never()).enregistrer(any(), any(), any(), any(), any());
    }

    @Test
    void notifierDestinataires_emet_une_notification_par_participant() {
        // Étant donné une liste explicite de participants
        UUID p1 = UUID.randomUUID();
        UUID p2 = UUID.randomUUID();
        UUID reunion = UUID.randomUUID();

        // Quand on les notifie
        service.notifierDestinataires(List.of(p1, p2), "reunion", "Convocation",
                "Message", "communication", reunion);

        // Alors un message est mis en file par participant
        verify(enregistreur, times(2)).enregistrer(eq("Notification"), any(UUID.class),
                eq(Topics.NOTIFICATIONS), eq("NotificationCreee"), any());
    }
}
