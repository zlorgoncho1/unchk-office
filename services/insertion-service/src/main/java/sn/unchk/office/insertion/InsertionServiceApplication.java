package sn.unchk.office.insertion;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Point d'entrée du microservice d'appui à l'insertion.
 * <p>
 * Le service gère les partenaires, les bilans de stages, le registre de contact
 * et les statistiques d'insertion (auto-emploi vs emploi salarié). Il émet le topic
 * {@code insertion.events} et consomme {@code people.students} et
 * {@code academic.formations} pour alimenter ses read-models locaux (CQRS).
 * <p>
 * L'auto-configuration de la librairie {@code common} apporte automatiquement la
 * sécurité JWT/JWKS, l'autorisation OPA anti-IDOR, la gestion d'erreurs web et la
 * configuration Kafka (producteur + consommateur).
 */
@SpringBootApplication
public class InsertionServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(InsertionServiceApplication.class, args);
    }
}
