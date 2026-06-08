package sn.unchk.office.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Point d'entrée de l'API Gateway (Spring Cloud Gateway RÉACTIF / WebFlux).
 *
 * <p>C'est le SEUL point d'entrée REST de la plateforme (tier Middle). La passerelle :</p>
 * <ul>
 *   <li>route les requêtes du frontend vers les microservices (par chemin /api/...) ;</li>
 *   <li>valide les JWT RS256 émis par identity-service (clés publiques via JWKS) ;</li>
 *   <li>demande l'autorisation à OPA (RBAC rôle × route, refus par défaut) ;</li>
 *   <li>ajoute les en-têtes de sécurité, applique le CORS en liste blanche,
 *       limite le débit par IP/sujet et propage un identifiant de corrélation.</li>
 * </ul>
 *
 * <p>La passerelle est entièrement réactive et NE dépend PAS de libs/common (qui est servlet) :
 * elle implémente ses propres filtres réactifs.</p>
 */
@SpringBootApplication
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
