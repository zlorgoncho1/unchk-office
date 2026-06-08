package sn.unchk.office.admin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Point d'entrée du service Administration (admin-service).
 * <p>
 * Périmètre : gestion budgétaire (projet de budget, note d'orientation, budget réalisé),
 * courrier arrivé/départ, notes de service et circulaires. Le service publie ses événements
 * budgétaires sur le topic {@code admin.budget} et consomme {@code people.staff} pour
 * alimenter son read-model local (zéro appel REST inter-service).
 * <p>
 * L'auto-configuration de la librairie {@code common} apporte la sécurité JWT/JWKS,
 * l'autorisation OPA anti-IDOR, la gestion d'erreurs web et la messagerie Kafka.
 */
@SpringBootApplication
public class AdminServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AdminServiceApplication.class, args);
    }
}
