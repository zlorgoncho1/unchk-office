package sn.unchk.office.people;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Point d'entree du people-service.
 * <p>
 * Service proprietaire des entites canoniques Etudiant et Personnel/Formateur.
 * Il expose le CRUD sous /api/people, emet les topics {@code people.students} et
 * {@code people.staff} a chaque changement, et maintient ses read-models locaux
 * (projection de {@code identity.users}) sans aucun appel REST inter-service.
 * <p>
 * L'auto-configuration de la librairie {@code common} (securite JWT/JWKS, OPA anti-IDOR,
 * gestion d'erreurs, Kafka) est importee automatiquement.
 */
@SpringBootApplication
public class PeopleServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PeopleServiceApplication.class, args);
    }
}
