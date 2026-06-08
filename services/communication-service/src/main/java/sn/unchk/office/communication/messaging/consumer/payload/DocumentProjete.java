package sn.unchk.office.communication.messaging.consumer.payload;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.UUID;

/**
 * Vue partielle de l'état d'un document reçu sur {@code document.documents}.
 * <p>
 * Sert uniquement à déclencher des notifications de circulaire : à la publication d'un
 * document de catégorie {@code circulaire}, on notifie les rôles de sa visibilité. Champs
 * inconnus ignorés.
 *
 * @param id          identifiant du document
 * @param title       titre / objet
 * @param category    catégorie (circulaire, note_service, courrier...)
 * @param published   indicateur de publication
 * @param visibility  rôles autorisés (destinataires de la circulaire)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record DocumentProjete(
        UUID id,
        String title,
        String category,
        Boolean published,
        List<String> visibility
) {
}
