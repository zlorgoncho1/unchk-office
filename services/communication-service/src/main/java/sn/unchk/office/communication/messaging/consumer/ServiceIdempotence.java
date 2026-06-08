package sn.unchk.office.communication.messaging.consumer;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.unchk.office.communication.domain.ProcessedEvent;
import sn.unchk.office.communication.repository.ProcessedEventRepository;

import java.util.UUID;

/**
 * Idempotence de consommation : un événement déjà traité n'est pas réappliqué.
 * <p>
 * Indispensable car les topics peuvent rejouer (reprise, nouveau consommateur, retry).
 * On enregistre l'{@code eventId} dans {@code processed_events} ; un doublon est ignoré.
 */
@Service
public class ServiceIdempotence {

    private final ProcessedEventRepository processedEventRepository;

    public ServiceIdempotence(ProcessedEventRepository processedEventRepository) {
        this.processedEventRepository = processedEventRepository;
    }

    /**
     * Tente d'enregistrer l'événement comme traité.
     *
     * @param eventId identifiant de l'événement (header {@code eventId})
     * @return {@code true} si c'est la première fois (à traiter), {@code false} si déjà vu
     */
    @Transactional
    public boolean marquerSiNouveau(UUID eventId) {
        if (eventId == null || processedEventRepository.existsById(eventId)) {
            return false;
        }
        processedEventRepository.save(new ProcessedEvent(eventId));
        return true;
    }
}
