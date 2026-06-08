package sn.unchk.office.academic.formation;

import java.util.UUID;

/**
 * Levée lorsqu'une formation n'existe pas (ou est supprimée logiquement).
 * <p>
 * Traduite en HTTP 404 par l'advice du service : sur une ressource identifiée par UUID,
 * on ne confirme jamais l'existence d'un objet inaccessible (anti-énumération).
 */
public class FormationIntrouvableException extends RuntimeException {

    public FormationIntrouvableException(UUID id) {
        super("Formation introuvable : " + id);
    }
}
