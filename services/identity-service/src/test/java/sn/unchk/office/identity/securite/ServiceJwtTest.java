package sn.unchk.office.identity.securite;

import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.Test;
import sn.unchk.office.identity.domaine.CleSignature;
import sn.unchk.office.identity.domaine.Utilisateur;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests d'émission des JWT RS256 : signature vérifiable, claims attendus.
 */
class ServiceJwtTest {

    @Test
    void leJetonEstSigneEnRs256EtPorteLesClaimsAttendus() throws Exception {
        // Préparation : propriétés d'émission et paire de clés (via le service de clés).
        JwtEmissionProprietes proprietes =
                new JwtEmissionProprietes("unchk-office", "unchk-office", 30, 7);
        ServiceJwt serviceJwt = new ServiceJwt(proprietes);

        // Pas de dépôt nécessaire ici : on n'appelle que les helpers de chargement de clé.
        ServiceCleSignature serviceCle = new ServiceCleSignature(null);
        // On fabrique une clé localement sans passer par la base.
        CleSignature cle = genererCle();
        RSAPrivateKey privee = serviceCle.clePrivee(cle);
        RSAPublicKey publique = serviceCle.clePublique(cle);

        Utilisateur utilisateur =
                Utilisateur.creer("awa@unchk.sn", "hash", "Awa Ba", null, null);

        // Émission du jeton.
        String compact = serviceJwt.emettreAccessToken(
                utilisateur, List.of("enseignant", "etudiant"), cle, privee);

        // Vérification : signature RS256 valide avec la clé publique.
        SignedJWT jwt = SignedJWT.parse(compact);
        assertTrue(jwt.verify(new RSASSAVerifier(publique)), "La signature doit être valide");

        // Claims : sujet = UUID, issuer/audience attendus, rôles présents, kid dans l'en-tête.
        assertEquals(utilisateur.getId().toString(), jwt.getJWTClaimsSet().getSubject());
        assertEquals("unchk-office", jwt.getJWTClaimsSet().getIssuer());
        assertTrue(jwt.getJWTClaimsSet().getAudience().contains("unchk-office"));
        assertEquals(List.of("enseignant", "etudiant"),
                jwt.getJWTClaimsSet().getStringListClaim("roles"));
        assertEquals(cle.getKid(), jwt.getHeader().getKeyID());
        assertNotNull(jwt.getJWTClaimsSet().getExpirationTime());
    }

    /** Génère une clé RSA en mémoire via les utilitaires PEM (sans base de données). */
    private CleSignature genererCle() throws Exception {
        var generateur = java.security.KeyPairGenerator.getInstance("RSA");
        generateur.initialize(2048);
        var paire = generateur.generateKeyPair();
        return CleSignature.creer(
                "kid-test",
                PemUtil.versPem(paire.getPublic()),
                PemUtil.versPem(paire.getPrivate()));
    }
}
