package sn.unchk.office.communication;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Point d'entrée du communication-service.
 * <p>
 * Service métier (Spring MVC servlet) responsable des comptes rendus, des réunions et des
 * notifications temps réel (push WebSocket). Il publie sur Kafka
 * {@code communication.comptesrendus}, {@code communication.reunions} et {@code notifications},
 * et consomme {@code identity.users} et {@code people.staff} pour ses read-models locaux (CQRS).
 * <p>
 * L'auto-configuration de {@code libs/common} apporte la sécurité JWT/JWKS, l'autorisation
 * OPA anti-IDOR, la gestion d'erreurs, la configuration Kafka et l'audit.
 * {@code @EnableScheduling} active le relais Outbox périodique.
 */
@SpringBootApplication
@EnableScheduling
public class CommunicationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CommunicationServiceApplication.class, args);
    }
}
