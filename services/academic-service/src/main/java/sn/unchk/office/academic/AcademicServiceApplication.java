package sn.unchk.office.academic;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Point d'entrée du academic-service.
 * <p>
 * Service du périmètre académique : formations (financement, formés par genre),
 * emplois du temps (créneaux) et affectation des formateurs. Il produit le topic
 * {@code academic.formations} et alimente une projection locale des formateurs
 * (read-model {@code academic_formateur_ro}) en consommant {@code people.staff},
 * sans aucun appel REST inter-service.
 * <p>
 * La librairie commune (auto-configurée) apporte la sécurité JWT/JWKS, l'autorisation
 * OPA anti-IDOR, la gestion d'erreurs, la configuration Kafka et l'export PDF/Excel.
 */
@SpringBootApplication
public class AcademicServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AcademicServiceApplication.class, args);
    }
}
