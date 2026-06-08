package sn.unchk.office.identity.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import sn.unchk.office.common.messaging.DomainEvent;
import sn.unchk.office.common.messaging.Topics;
import sn.unchk.office.identity.depot.EvenementTraiteRepository;
import sn.unchk.office.identity.depot.ReadPersonRepository;
import sn.unchk.office.identity.domaine.EvenementTraite;
import sn.unchk.office.identity.domaine.ReadPerson;

import java.util.Map;
import java.util.UUID;

/**
 * Consommateur Kafka alimentant le read-model local {@code read_person}.
 * <p>
 * Conforme au CQRS : identity-service ne fait AUCUN appel REST vers people-service ; il
 * maintient une projection locale des personnes canoniques en consommant {@code people.students}
 * et {@code people.staff}. Cette projection sert à valider le {@code person_ref} d'un compte.
 * <p>
 * Chaque message est dédoublonné sur son {@code eventId} (table {@code processed_events})
 * pour garantir l'idempotence en cas de rejeu du topic.
 */
@Component
public class ConsommateurPersonnes {

    private static final Logger log = LoggerFactory.getLogger(ConsommateurPersonnes.class);

    private final ReadPersonRepository depotPersonnes;
    private final EvenementTraiteRepository depotEvenements;

    public ConsommateurPersonnes(ReadPersonRepository depotPersonnes,
                                 EvenementTraiteRepository depotEvenements) {
        this.depotPersonnes = depotPersonnes;
        this.depotEvenements = depotEvenements;
    }

    /** Projette les étudiants canoniques (people.students) en lecture seule. */
    @KafkaListener(topics = Topics.PEOPLE_STUDENTS, groupId = "identity-service")
    @Transactional
    public void surEtudiant(DomainEvent<Map<String, Object>> evenement) {
        projeter(evenement, "etudiant");
    }

    /** Projette le personnel canonique (people.staff) en lecture seule. */
    @KafkaListener(topics = Topics.PEOPLE_STAFF, groupId = "identity-service")
    @Transactional
    public void surPersonnel(DomainEvent<Map<String, Object>> evenement) {
        projeter(evenement, "personnel");
    }

    /**
     * Applique un évènement à la projection locale, de façon idempotente.
     *
     * @param evenement enveloppe reçue
     * @param kind      nature de la personne (etudiant / personnel)
     */
    private void projeter(DomainEvent<Map<String, Object>> evenement, String kind) {
        if (evenement == null || evenement.eventId() == null || evenement.payload() == null) {
            log.warn("Évènement people ignoré (enveloppe incomplète).");
            return;
        }
        // Idempotence : on ignore un évènement déjà traité (rejeu du topic).
        if (depotEvenements.existsById(evenement.eventId())) {
            return;
        }

        Map<String, Object> payload = evenement.payload();
        UUID id = lireUuid(payload, "id");
        if (id == null) {
            log.warn("Évènement people sans identifiant : ignoré.");
            return;
        }

        String type = evenement.eventType();
        if (type != null && type.toLowerCase().contains("delete")) {
            // Tombstone logique : on retire la personne de la projection.
            depotPersonnes.deleteById(id);
        } else {
            ReadPerson personne = depotPersonnes.findById(id)
                    .orElseGet(() -> new ReadPerson(id, kind));
            personne.setPersonKind(kind);
            personne.mettreAJour(nomComplet(payload), lireTexte(payload, "email"));
            depotPersonnes.save(personne);
        }

        depotEvenements.save(new EvenementTraite(evenement.eventId()));
    }

    /** Compose le nom complet à partir des champs possibles du payload. */
    private String nomComplet(Map<String, Object> payload) {
        String complet = lireTexte(payload, "fullName");
        if (complet != null) {
            return complet;
        }
        String prenom = lireTexte(payload, "firstName");
        String nom = lireTexte(payload, "lastName");
        if (prenom == null && nom == null) {
            return null;
        }
        return ((prenom != null ? prenom : "") + " " + (nom != null ? nom : "")).trim();
    }

    private String lireTexte(Map<String, Object> payload, String cle) {
        Object valeur = payload.get(cle);
        return valeur != null ? valeur.toString() : null;
    }

    private UUID lireUuid(Map<String, Object> payload, String cle) {
        Object valeur = payload.get(cle);
        if (valeur == null) {
            return null;
        }
        try {
            return UUID.fromString(valeur.toString());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
