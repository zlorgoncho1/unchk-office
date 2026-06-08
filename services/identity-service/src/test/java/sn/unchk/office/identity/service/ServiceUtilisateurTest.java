package sn.unchk.office.identity.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import sn.unchk.office.common.audit.AuditLogger;
import sn.unchk.office.identity.depot.RefreshTokenRepository;
import sn.unchk.office.identity.depot.RoleUtilisateurRepository;
import sn.unchk.office.identity.depot.UtilisateurRepository;
import sn.unchk.office.identity.domaine.Utilisateur;
import sn.unchk.office.identity.dto.RequeteCreationUtilisateur;
import sn.unchk.office.identity.dto.VueUtilisateur;
import sn.unchk.office.identity.messaging.ProducteurUtilisateur;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests du service de gestion des comptes : création (avec émission Kafka) et conflit d'email.
 */
class ServiceUtilisateurTest {

    private UtilisateurRepository depotUtilisateurs;
    private RoleUtilisateurRepository depotRoles;
    private RefreshTokenRepository depotRefresh;
    private ProducteurUtilisateur producteur;
    private AuditLogger auditLogger;
    private ServiceUtilisateur service;

    @BeforeEach
    void preparer() {
        depotUtilisateurs = mock(UtilisateurRepository.class);
        depotRoles = mock(RoleUtilisateurRepository.class);
        depotRefresh = mock(RefreshTokenRepository.class);
        producteur = mock(ProducteurUtilisateur.class);
        auditLogger = mock(AuditLogger.class);
        PasswordEncoder encodeur = new BCryptPasswordEncoder();

        // On renvoie les rôles tels que persistés (saveAll renvoie l'argument).
        when(depotRoles.saveAll(any())).thenAnswer(inv -> new java.util.ArrayList<>(inv.getArgument(0)));

        service = new ServiceUtilisateur(
                depotUtilisateurs, depotRoles, depotRefresh, encodeur, producteur, auditLogger);
    }

    @Test
    void laCreationPersisteLeCompteEtEmetIdentityUsers() {
        when(depotUtilisateurs.existsByEmail("nouvel@unchk.sn")).thenReturn(false);

        RequeteCreationUtilisateur requete = new RequeteCreationUtilisateur(
                "nouvel@unchk.sn", "motDePasse123", "Nouvel Utilisateur",
                List.of("enseignant"), null, null);

        VueUtilisateur vue = service.creer(requete, null);

        // Le compte est sauvegardé, l'évènement de création est émis.
        assertEquals("nouvel@unchk.sn", vue.email());
        assertEquals(List.of("enseignant"), vue.roles());
        verify(depotUtilisateurs).save(any(Utilisateur.class));
        verify(producteur).publierCree(any());
    }

    @Test
    void unCourrielDejaPrisDeclencheUnConflit() {
        when(depotUtilisateurs.existsByEmail("existe@unchk.sn")).thenReturn(true);

        RequeteCreationUtilisateur requete = new RequeteCreationUtilisateur(
                "existe@unchk.sn", "motDePasse123", "Doublon",
                List.of("etudiant"), null, null);

        // Email déjà utilisé : conflit 409, aucun compte créé ni évènement émis.
        assertThrows(ConflitException.class, () -> service.creer(requete, null));
        verify(depotUtilisateurs, never()).save(any());
        verify(producteur, never()).publierCree(any());
    }

    @Test
    void leHashDuMotDePasseNApparaitJamaisDansLaVue() {
        when(depotUtilisateurs.existsByEmail(any())).thenReturn(false);
        RequeteCreationUtilisateur requete = new RequeteCreationUtilisateur(
                "x@unchk.sn", "motDePasse123", "X", List.of("admin"), null, null);

        VueUtilisateur vue = service.creer(requete, null);
        // La vue publique ne doit pas exposer le mot de passe en clair.
        assertFalse(vue.toString().contains("motDePasse123"));
    }
}
