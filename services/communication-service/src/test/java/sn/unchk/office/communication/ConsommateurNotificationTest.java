package sn.unchk.office.communication;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import sn.unchk.office.common.messaging.DomainEvent;
import sn.unchk.office.communication.domain.Notification;
import sn.unchk.office.communication.domain.NotificationKind;
import sn.unchk.office.communication.messaging.consumer.ConsommateurNotification;
import sn.unchk.office.communication.messaging.consumer.ServiceIdempotence;
import sn.unchk.office.communication.messaging.payload.NotificationEvent;
import sn.unchk.office.communication.repository.NotificationRepository;
import sn.unchk.office.communication.ws.PousseurNotificationWs;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests unitaires du consommateur du topic notifications.
 * Vérifie la persistance + tentative de push WebSocket, et le respect de l'idempotence.
 */
@ExtendWith(MockitoExtension.class)
class ConsommateurNotificationTest {

    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private PousseurNotificationWs pousseur;
    @Mock
    private ServiceIdempotence idempotence;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private ConsommateurNotification consommateur;

    private ConsommateurNotification creer() {
        return new ConsommateurNotification(notificationRepository, pousseur, idempotence, objectMapper);
    }

    @Test
    void consommer_persiste_la_notification_et_tente_le_push() {
        // Étant donné un événement notification non encore traité
        consommateur = creer();
        UUID recipient = UUID.randomUUID();
        NotificationEvent payload = new NotificationEvent(
                recipient, NotificationKind.compte_rendu.name(), "Titre", "Message",
                "communication", UUID.randomUUID());
        DomainEvent<Object> enveloppe = DomainEvent.creer("NotificationCreee", "trace-1", payload);
        ConsumerRecord<String, DomainEvent<Object>> record =
                new ConsumerRecord<>("notifications", 0, 0L, recipient.toString(), enveloppe);
        when(idempotence.marquerSiNouveau(any())).thenReturn(true);
        when(pousseur.pousser(any())).thenReturn(true);

        // Quand on consomme
        consommateur.consommer(record);

        // Alors la notification est persistée avec le bon destinataire et marquée livrée WS
        ArgumentCaptor<Notification> capteur = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(capteur.capture());
        Notification persistee = capteur.getValue();
        assertThat(persistee.getRecipientId()).isEqualTo(recipient);
        assertThat(persistee.getKind()).isEqualTo(NotificationKind.compte_rendu);
        assertThat(persistee.isDeliveredWs()).isTrue();
        verify(pousseur).pousser(any());
    }

    @Test
    void consommer_evenement_deja_traite_est_ignore() {
        // Étant donné un événement déjà traité (idempotence)
        consommateur = creer();
        NotificationEvent payload = new NotificationEvent(
                UUID.randomUUID(), "systeme", "T", "M", "communication", null);
        DomainEvent<Object> enveloppe = DomainEvent.creer("NotificationCreee", null, payload);
        ConsumerRecord<String, DomainEvent<Object>> record =
                new ConsumerRecord<>("notifications", 0, 0L, "k", enveloppe);
        when(idempotence.marquerSiNouveau(any())).thenReturn(false);

        // Quand on consomme / Alors rien n'est persisté ni poussé
        consommateur.consommer(record);

        verify(notificationRepository, never()).save(any());
        verify(pousseur, never()).pousser(any());
    }
}
