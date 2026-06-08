package sn.unchk.office.academic.formateur;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Affectation d'un formateur à une formation pour une matière (module) donnée.
 * <p>
 * Table {@code formation_formateurs}, clé composite {@link AffectationFormateurId}.
 * Le formateur est désigné par une référence logique vers {@code people.staff.id}
 * (résolu localement via le read-model {@code academic_formateur_ro}, jamais en REST).
 */
@Entity
@Table(name = "formation_formateurs")
public class AffectationFormateur {

    /** Clé composite (formation, formateur, module). */
    @EmbeddedId
    private AffectationFormateurId id;

    /** Date d'affectation. */
    @Column(name = "assigned_at", nullable = false)
    private Instant assignedAt;

    protected AffectationFormateur() {
        // Constructeur requis par JPA.
    }

    public AffectationFormateur(UUID formationId, UUID formateurRef, String module) {
        this.id = new AffectationFormateurId(formationId, formateurRef, module);
    }

    /** Renseigne la date d'affectation avant la première persistance. */
    @PrePersist
    void avantInsertion() {
        if (assignedAt == null) {
            assignedAt = Instant.now();
        }
    }

    public AffectationFormateurId getId() {
        return id;
    }

    public UUID getFormationId() {
        return id != null ? id.getFormationId() : null;
    }

    public UUID getFormateurRef() {
        return id != null ? id.getFormateurRef() : null;
    }

    public String getModule() {
        return id != null ? id.getModule() : null;
    }

    public Instant getAssignedAt() {
        return assignedAt;
    }
}
