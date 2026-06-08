package sn.unchk.office.identity.messaging;

import org.junit.jupiter.api.Test;
import sn.unchk.office.identity.domaine.RoleCode;
import sn.unchk.office.identity.domaine.RoleUtilisateur;
import sn.unchk.office.identity.domaine.Utilisateur;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Tests de la charge utile publiée sur identity.users : statut déduit et absence de secret.
 */
class UtilisateurEvenementTest {

    @Test
    void unCompteActifEstPublieAvecLeStatutActive() {
        Utilisateur u = Utilisateur.creer("a@unchk.sn", "hash-secret", "Awa Ba", null, null);
        UtilisateurEvenement etat = UtilisateurEvenement.depuis(
                u, List.of(RoleUtilisateur.creer(u.getId(), RoleCode.ETUDIANT, null)));

        assertEquals(UtilisateurEvenement.STATUT_ACTIF, etat.status());
        assertEquals(List.of("etudiant"), etat.roles());
    }

    @Test
    void unCompteVerrouilleEstPublieAvecLeStatutSuspended() {
        Utilisateur u = Utilisateur.creer("a@unchk.sn", "hash", "Awa Ba", null, null);
        u.setLocked(true);
        UtilisateurEvenement etat = UtilisateurEvenement.depuis(u, List.of());
        assertEquals(UtilisateurEvenement.STATUT_SUSPENDU, etat.status());
    }

    @Test
    void unCompteSupprimeEstPublieAvecLeStatutDisabled() {
        Utilisateur u = Utilisateur.creer("a@unchk.sn", "hash", "Awa Ba", null, null);
        u.setDeletedAt(Instant.now());
        UtilisateurEvenement etat = UtilisateurEvenement.depuis(u, List.of());
        assertEquals(UtilisateurEvenement.STATUT_DESACTIVE, etat.status());
    }

    @Test
    void aucunSecretNApparaitDansLEvenement() {
        // Sécurité : la charge utile ne doit transporter ni hash ni mot de passe.
        Utilisateur u = Utilisateur.creer("a@unchk.sn", "hash-secret-tres-sensible", "Awa Ba", null, null);
        UtilisateurEvenement etat = UtilisateurEvenement.depuis(u, List.of());
        // On vérifie qu'aucun champ ne contient la valeur du hash.
        assertFalse(etat.toString().contains("hash-secret-tres-sensible"));
    }
}
