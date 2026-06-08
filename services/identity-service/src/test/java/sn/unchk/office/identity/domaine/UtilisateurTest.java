package sn.unchk.office.identity.domaine;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests du comportement métier du compte utilisateur (verrouillage, utilisabilité).
 */
class UtilisateurTest {

    @Test
    void unNouveauCompteEstActifEtUtilisable() {
        Utilisateur u = Utilisateur.creer("a@unchk.sn", "hash", "Awa Ba", null, null);
        // Un compte fraîchement créé est actif, non verrouillé et utilisable.
        assertTrue(u.isActive());
        assertFalse(u.isLocked());
        assertTrue(u.estUtilisable());
    }

    @Test
    void leCompteSeVerrouilleAuSeuilDEchecs() {
        Utilisateur u = Utilisateur.creer("a@unchk.sn", "hash", "Awa Ba", null, null);
        // Trois échecs avec un seuil de 3 doivent verrouiller le compte.
        u.connexionEchouee(3);
        u.connexionEchouee(3);
        assertFalse(u.isLocked());
        u.connexionEchouee(3);
        assertTrue(u.isLocked());
        assertFalse(u.estUtilisable());
    }

    @Test
    void uneConnexionReussieRemetLesEchecsAZero() {
        Utilisateur u = Utilisateur.creer("a@unchk.sn", "hash", "Awa Ba", null, null);
        u.connexionEchouee(5);
        u.connexionEchouee(5);
        u.connexionReussie();
        // Après un succès, le compteur d'échecs est remis à zéro.
        assertEquals(0, u.getFailedAttempts());
    }
}
