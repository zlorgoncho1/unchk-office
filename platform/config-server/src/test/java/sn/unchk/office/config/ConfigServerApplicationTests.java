package sn.unchk.office.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Test de fumée du Config Server.
 *
 * <p>Vérifie que le contexte Spring démarre correctement avec le profil natif
 * (configuration servie depuis le classpath). Si le serveur ne se charge pas,
 * ce test échoue et signale une régression de configuration.</p>
 */
@SpringBootTest
class ConfigServerApplicationTests {

    /**
     * Le contexte applicatif doit se charger sans erreur.
     */
    @Test
    void leContexteSeCharge() {
        // Aucune assertion nécessaire : l'échec du chargement fait échouer le test.
    }
}
