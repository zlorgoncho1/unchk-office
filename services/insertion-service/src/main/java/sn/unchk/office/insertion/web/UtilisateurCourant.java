package sn.unchk.office.insertion.web;

import sn.unchk.office.common.authz.ContexteSecurite;
import sn.unchk.office.common.authz.EntreeOpa;

import java.util.UUID;

/**
 * Petit utilitaire pour récupérer l'identifiant de l'utilisateur authentifié (claim {@code sub}).
 * <p>
 * Sert à renseigner {@code createdBy} (= ownerId ABAC) sur les entités créées, sans jamais
 * lier ce champ système depuis le corps de la requête (anti sur-affectation / mass assignment).
 */
public final class UtilisateurCourant {

    private UtilisateurCourant() {
        // Classe utilitaire.
    }

    /**
     * @return l'UUID de l'utilisateur courant, ou {@code null} si l'identité n'est pas un UUID.
     */
    public static UUID id() {
        EntreeOpa.Sujet sujet = ContexteSecurite.sujetCourant();
        if (sujet.id() == null) {
            return null;
        }
        try {
            return UUID.fromString(sujet.id());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
