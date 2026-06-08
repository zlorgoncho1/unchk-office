package sn.unchk.office.identity.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import sn.unchk.office.identity.depot.AuditAuthRepository;
import sn.unchk.office.identity.depot.RefreshTokenRepository;
import sn.unchk.office.identity.depot.RoleUtilisateurRepository;
import sn.unchk.office.identity.depot.UtilisateurRepository;
import sn.unchk.office.identity.domaine.CleSignature;
import sn.unchk.office.identity.domaine.RefreshToken;
import sn.unchk.office.identity.domaine.RoleCode;
import sn.unchk.office.identity.domaine.RoleUtilisateur;
import sn.unchk.office.identity.domaine.Utilisateur;
import sn.unchk.office.identity.dto.ReponseJetons;
import sn.unchk.office.identity.messaging.ProducteurUtilisateur;
import sn.unchk.office.identity.securite.JwtEmissionProprietes;
import sn.unchk.office.identity.securite.PemUtil;
import sn.unchk.office.identity.securite.SecuriteProprietes;
import sn.unchk.office.identity.securite.ServiceCleSignature;
import sn.unchk.office.identity.securite.ServiceJwt;

import java.security.KeyPairGenerator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests du service d'authentification : connexion réussie, mauvais mot de passe avec
 * verrouillage anti-bruteforce, et rafraîchissement avec rotation du refresh token.
 */
class ServiceAuthentificationTest {

    private UtilisateurRepository depotUtilisateurs;
    private RoleUtilisateurRepository depotRoles;
    private RefreshTokenRepository depotRefresh;
    private AuditAuthRepository depotAudit;
    private ProducteurUtilisateur producteur;
    private ServiceCleSignature serviceCle;
    private PasswordEncoder encodeur;
    private ServiceAuthentification service;

    private Utilisateur utilisateur;
    private final String motDePasse = "motDePasse123";

    @BeforeEach
    void preparer() throws Exception {
        depotUtilisateurs = mock(UtilisateurRepository.class);
        depotRoles = mock(RoleUtilisateurRepository.class);
        depotRefresh = mock(RefreshTokenRepository.class);
        depotAudit = mock(AuditAuthRepository.class);
        producteur = mock(ProducteurUtilisateur.class);
        serviceCle = mock(ServiceCleSignature.class);
        encodeur = new BCryptPasswordEncoder();

        ServiceJwt serviceJwt = new ServiceJwt(
                new JwtEmissionProprietes("unchk-office", "unchk-office", 30, 7));

        // Compte de test avec mot de passe haché en BCrypt.
        utilisateur = Utilisateur.creer(
                "awa@unchk.sn", encodeur.encode(motDePasse), "Awa Ba", null, null);

        // Clé de signature réelle (en mémoire) pour produire un vrai JWT.
        var generateur = KeyPairGenerator.getInstance("RSA");
        generateur.initialize(2048);
        var paire = generateur.generateKeyPair();
        CleSignature cle = CleSignature.creer(
                "kid-test", PemUtil.versPem(paire.getPublic()), PemUtil.versPem(paire.getPrivate()));
        when(serviceCle.cleActive()).thenReturn(cle);
        when(serviceCle.clePrivee(any())).thenReturn((java.security.interfaces.RSAPrivateKey) paire.getPrivate());

        when(depotRoles.findByIdUserId(any()))
                .thenReturn(List.of(RoleUtilisateur.creer(utilisateur.getId(), RoleCode.ENSEIGNANT, null)));

        service = new ServiceAuthentification(
                depotUtilisateurs, depotRoles, depotRefresh, depotAudit,
                encodeur, serviceJwt, serviceCle, producteur,
                new SecuriteProprietes(3));
    }

    @Test
    void uneConnexionValideDelivreUnAccessEtUnRefreshToken() {
        when(depotUtilisateurs.findByEmail("awa@unchk.sn")).thenReturn(Optional.of(utilisateur));

        ReponseJetons jetons = service.connexion("awa@unchk.sn", motDePasse, "127.0.0.1", "JUnit");

        // On obtient bien une paire de jetons et les rôles attendus.
        assertNotNull(jetons.accessToken());
        assertNotNull(jetons.refreshToken());
        assertEquals("Bearer", jetons.tokenType());
        assertTrue(jetons.roles().contains("enseignant"));
        // Le refresh token est persisté (sous forme de hash).
        verify(depotRefresh).save(any(RefreshToken.class));
    }

    @Test
    void unMauvaisMotDePasseEstRefuseEtIncrementeLesEchecs() {
        when(depotUtilisateurs.findByEmail("awa@unchk.sn")).thenReturn(Optional.of(utilisateur));

        // Le mot de passe erroné doit lever une exception d'authentification.
        assertThrows(AuthentificationException.class,
                () -> service.connexion("awa@unchk.sn", "mauvais", "127.0.0.1", "JUnit"));

        // Le compteur d'échecs a été incrémenté et le compte sauvegardé.
        assertEquals(1, utilisateur.getFailedAttempts());
        verify(depotUtilisateurs).save(utilisateur);
        // Aucun refresh token n'est délivré en cas d'échec.
        verify(depotRefresh, never()).save(any(RefreshToken.class));
    }

    @Test
    void leCompteSeVerrouilleApresTropDEchecsEtPublieSonStatut() {
        when(depotUtilisateurs.findByEmail("awa@unchk.sn")).thenReturn(Optional.of(utilisateur));

        // Seuil = 3 : on provoque trois échecs consécutifs.
        for (int i = 0; i < 3; i++) {
            assertThrows(AuthentificationException.class,
                    () -> service.connexion("awa@unchk.sn", "mauvais", "127.0.0.1", "JUnit"));
        }

        // Le compte est verrouillé et le changement de statut est publié sur identity.users.
        assertTrue(utilisateur.isLocked());
        verify(producteur).publierMisAJour(any());
    }

    @Test
    void unCompteInconnuEstRefuseAvecUnMessageGenerique() {
        when(depotUtilisateurs.findByEmail("inconnu@unchk.sn")).thenReturn(Optional.empty());

        // Email inconnu : refus (anti-énumération, aucune fuite côté client).
        assertThrows(AuthentificationException.class,
                () -> service.connexion("inconnu@unchk.sn", motDePasse, "127.0.0.1", "JUnit"));
    }

    @Test
    void unRefreshTokenValideEstEchangeEtRoule() {
        String refreshBrut = "jeton-refresh-brut";
        String hash = HashUtil.sha256(refreshBrut);
        RefreshToken jeton = RefreshToken.creer(
                utilisateur.getId(), hash, java.time.Instant.now().plusSeconds(3600));

        when(depotRefresh.findByTokenHash(hash)).thenReturn(Optional.of(jeton));
        when(depotUtilisateurs.findById(utilisateur.getId())).thenReturn(Optional.of(utilisateur));

        ReponseJetons jetons = service.rafraichir(refreshBrut, "127.0.0.1", "JUnit");

        // Un nouveau jeton est délivré et l'ancien refresh est révoqué (rotation anti-rejeu).
        assertNotNull(jetons.accessToken());
        assertNotNull(jeton.getRevokedAt());
    }

    @Test
    void unRefreshTokenInconnuEstRefuse() {
        when(depotRefresh.findByTokenHash(any())).thenReturn(Optional.empty());

        assertThrows(AuthentificationException.class,
                () -> service.rafraichir("inconnu", "127.0.0.1", "JUnit"));
    }
}
