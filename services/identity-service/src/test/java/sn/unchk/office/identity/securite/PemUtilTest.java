package sn.unchk.office.identity.securite;

import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

/**
 * Tests de conversion des clés RSA vers PEM et inversement.
 */
class PemUtilTest {

    @Test
    void uneCleRsaSurvitAUnAllerRetourPem() throws Exception {
        KeyPairGenerator generateur = KeyPairGenerator.getInstance("RSA");
        generateur.initialize(2048);
        KeyPair paire = generateur.generateKeyPair();

        // Conversion clé -> PEM -> clé : le contenu encodé doit être identique.
        String pemPublic = PemUtil.versPem(paire.getPublic());
        String pemPrivate = PemUtil.versPem(paire.getPrivate());

        PublicKey publiqueRelue = PemUtil.clePubliqueDepuisPem(pemPublic);
        PrivateKey priveeRelue = PemUtil.clePriveeDepuisPem(pemPrivate);

        assertArrayEquals(paire.getPublic().getEncoded(), publiqueRelue.getEncoded());
        assertArrayEquals(paire.getPrivate().getEncoded(), priveeRelue.getEncoded());
    }
}
