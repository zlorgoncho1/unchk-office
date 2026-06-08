package sn.unchk.office.identity.domaine;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests du type énuméré des rôles.
 */
class RoleCodeTest {

    @Test
    void leLibelleCanoniqueEstConserve() {
        // On vérifie que les libellés correspondent EXACTEMENT à ceux attendus par OPA.
        assertEquals("appui-insertion", RoleCode.APPUI_INSERTION.libelle());
        assertEquals("etudiant", RoleCode.ETUDIANT.libelle());
        assertEquals("admin", RoleCode.ADMIN.libelle());
    }

    @Test
    void leLibelleEstReconnuSansTenirCompteDeLaCasse() {
        // La conversion tolère la casse et les espaces autour.
        assertEquals(RoleCode.ENSEIGNANT, RoleCode.depuisLibelle("  Enseignant "));
    }

    @Test
    void unLibelleInconnuEstRejete() {
        // Un rôle inconnu doit lever une exception (deny-by-default applicatif).
        assertThrows(IllegalArgumentException.class, () -> RoleCode.depuisLibelle("super-admin"));
    }
}
