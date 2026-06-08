package sn.unchk.office.people.messaging.consumer;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import sn.unchk.office.common.messaging.DomainEvent;
import sn.unchk.office.common.messaging.Topics;
import sn.unchk.office.people.domain.IdentityUserRo;
import sn.unchk.office.people.domain.ProcessedEvent;
import sn.unchk.office.people.repository.IdentityUserRoRepository;
import sn.unchk.office.people.repository.ProcessedEventRepository;
import sn.unchk.office.people.repository.StudentRepository;

import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;

/**
 * Consommateur du topic {@code identity.users} : alimente le read-model local
 * {@link IdentityUserRo} (projection CQRS).
 * <p>
 * AUCUN appel REST vers identity-service : people-service projette localement les
 * comptes utilisateurs pour relier un etudiant a son compte (acces "me" anti-IDOR)
 * et afficher l'auteur des fiches. L'idempotence repose sur {@code eventId}
 * (table {@code processed_events}) : un evenement rejoue est ignore.
 */
@Component
public class IdentityUserConsumer {

    private static final Logger log = LoggerFactory.getLogger(IdentityUserConsumer.class);

    private final IdentityUserRoRepository userRoRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final StudentRepository studentRepository;

    public IdentityUserConsumer(IdentityUserRoRepository userRoRepository,
                                ProcessedEventRepository processedEventRepository,
                                StudentRepository studentRepository) {
        this.userRoRepository = userRoRepository;
        this.processedEventRepository = processedEventRepository;
        this.studentRepository = studentRepository;
    }

    /**
     * Traite un evenement {@code identity.users}.
     * <p>
     * Le {@code DomainEvent} est desserialise avec un payload generique (Map JSON).
     * On en extrait l'etat du compte pour faire un upsert idempotent du read-model.
     */
    @KafkaListener(topics = Topics.IDENTITY_USERS, groupId = "people-service")
    @Transactional
    public void consommer(ConsumerRecord<String, DomainEvent<Map<String, Object>>> record) {
        DomainEvent<Map<String, Object>> evenement = record.value();
        if (evenement == null) {
            // Tombstone Kafka (valeur null sur topic compacte) : on purge la cle.
            supprimerParCle(record.key());
            return;
        }

        UUID eventId = evenement.eventId();
        // Idempotence : on ne traite jamais deux fois le meme evenement.
        if (eventId != null && processedEventRepository.existsByEventId(eventId)) {
            log.debug("Evenement identity.users deja traite, ignore : {}", eventId);
            return;
        }

        String typeEvenement = evenement.eventType();
        Map<String, Object> payload = evenement.payload();

        if ("Deleted".equals(typeEvenement) || payload == null) {
            UUID id = lireUuid(payload != null ? payload.get("id") : null, record.key());
            if (id != null) {
                userRoRepository.deleteById(id);
            }
        } else {
            appliquerUpsert(payload, record.offset());
        }

        if (eventId != null) {
            processedEventRepository.save(new ProcessedEvent(eventId));
        }
    }

    /** Upsert du read-model a partir de l'etat du compte porte par le payload. */
    private void appliquerUpsert(Map<String, Object> payload, long offset) {
        UUID id = lireUuid(payload.get("id"), null);
        if (id == null) {
            // Sans identifiant exploitable on ne peut rien projeter : on ignore proprement.
            log.warn("Evenement identity.users sans identifiant exploitable, ignore.");
            return;
        }

        IdentityUserRo vue = userRoRepository.findById(id).orElseGet(IdentityUserRo::new);
        vue.setId(id);
        vue.setFullName(lireTexte(payload.get("fullName"), "(inconnu)"));
        vue.setEmail(lireTexteNullable(payload.get("email")));
        vue.setRoles(lireRoles(payload.get("roles")));
        vue.setPersonRef(lireUuid(payload.get("personRef"), null));
        vue.setActive(lireBooleen(payload.get("isActive"), true));
        vue.setLastEventAt(Instant.now());
        vue.setEventOffset(offset);
        userRoRepository.save(vue);

        // Lie l'etudiant a son compte : si le compte reference une personne (personRef)
        // correspondant a un etudiant, on renseigne sa colonne user_ref. Cela permet la
        // resolution cote serveur de /api/etudiants/me (anti-IDOR). No-op si personRef
        // vise un personnel (aucun etudiant ne correspond a cet id).
        UUID personRef = vue.getPersonRef();
        if (personRef != null) {
            studentRepository.findById(personRef).ifPresent(etudiant -> {
                if (!id.equals(etudiant.getUserRef())) {
                    etudiant.setUserRef(id);
                    studentRepository.save(etudiant);
                }
            });
        }
    }

    private void supprimerParCle(String cle) {
        UUID id = lireUuid(cle, null);
        if (id != null) {
            userRoRepository.deleteById(id);
        }
    }

    // --- Aides de lecture defensive du payload JSON ---

    private UUID lireUuid(Object valeur, String repli) {
        Object source = valeur != null ? valeur : repli;
        if (source == null) {
            return null;
        }
        try {
            return UUID.fromString(source.toString());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private String lireTexte(Object valeur, String repli) {
        return valeur != null ? valeur.toString() : repli;
    }

    private String lireTexteNullable(Object valeur) {
        return valeur != null ? valeur.toString() : null;
    }

    private boolean lireBooleen(Object valeur, boolean repli) {
        if (valeur instanceof Boolean b) {
            return b;
        }
        return valeur != null ? Boolean.parseBoolean(valeur.toString()) : repli;
    }

    /** Convertit la liste de roles JSON en tableau texte (pour la colonne {@code roles}). */
    private String[] lireRoles(Object valeur) {
        if (valeur instanceof Collection<?> collection) {
            return collection.stream().map(String::valueOf).toArray(String[]::new);
        }
        return new String[0];
    }
}
