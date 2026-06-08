package sn.unchk.office.identity.securite;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.ApplicationArguments;
import org.springframework.stereotype.Component;

/**
 * Initialisation au démarrage : garantit qu'une clé de signature RSA active existe.
 * <p>
 * Sans clé active, l'émission de JWT et l'exposition du JWKS seraient impossibles. Ce runner
 * génère et persiste une paire RSA si la table {@code signing_keys} est vide.
 */
@Component
public class DemarrageIdentity implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemarrageIdentity.class);

    private final ServiceCleSignature serviceCle;

    public DemarrageIdentity(ServiceCleSignature serviceCle) {
        this.serviceCle = serviceCle;
    }

    @Override
    public void run(ApplicationArguments args) {
        serviceCle.assurerCleActive();
        log.info("identity-service : clé de signature active prête (JWKS disponible).");
    }
}
