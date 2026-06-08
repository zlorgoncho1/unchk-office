package sn.unchk.office.communication.messaging.consumer.payload;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.UUID;

/**
 * Vue partielle de l'état d'un utilisateur reçu sur {@code identity.users}.
 * <p>
 * On ne lit que les champs nécessaires à la projection {@code identity_user_ro} ; les champs
 * inconnus sont ignorés (compatibilité ascendante avec les évolutions additives du schéma).
 *
 * @param id       identifiant de l'utilisateur (identity.users.id)
 * @param fullName nom complet
 * @param email    courriel
 * @param roles    rôles applicatifs
 * @param active   compte actif ou non (statut ACTIVE)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record UtilisateurProjete(
        UUID id,
        String fullName,
        String email,
        List<String> roles,
        Boolean active
) {
}
