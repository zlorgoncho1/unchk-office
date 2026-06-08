package sn.unchk.office.academic.formateur;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Read-model (projection CQRS, lecture seule) des formateurs.
 * <p>
 * Table {@code academic_formateur_ro}, alimentée en consommant le topic {@code people.staff}.
 * Elle permet d'afficher les noms / spécialités des formateurs sans aucun appel REST vers
 * people-service. Cette table n'est JAMAIS écrite par l'API : seul le consommateur Kafka
 * la met à jour (upsert idempotent).
 */
@Entity
@Table(name = "academic_formateur_ro")
public class FormateurRo {

    /** Identifiant du formateur = people.staff.id. */
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /** Nom complet affiché. */
    @Column(name = "full_name", nullable = false)
    private String fullName;

    /** Type de personnel (enseignant, tuteur...). */
    @Column(name = "kind", nullable = false)
    private String kind;

    /** Spécialité du formateur, optionnelle. */
    @Column(name = "speciality")
    private String speciality;

    /** Formateur en activité. */
    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    /** Horodatage du dernier événement appliqué (pour le suivi de fraîcheur). */
    @Column(name = "last_event_at", nullable = false)
    private Instant lastEventAt;

    /** Offset Kafka du dernier événement appliqué (idempotence / ordonnancement). */
    @Column(name = "event_offset")
    private Long eventOffset;

    protected FormateurRo() {
        // Constructeur requis par JPA.
    }

    public FormateurRo(UUID id) {
        this.id = id;
    }

    // --- Accesseurs / mutateurs ---

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public String getSpeciality() {
        return speciality;
    }

    public void setSpeciality(String speciality) {
        this.speciality = speciality;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Instant getLastEventAt() {
        return lastEventAt;
    }

    public void setLastEventAt(Instant lastEventAt) {
        this.lastEventAt = lastEventAt;
    }

    public Long getEventOffset() {
        return eventOffset;
    }

    public void setEventOffset(Long eventOffset) {
        this.eventOffset = eventOffset;
    }
}
