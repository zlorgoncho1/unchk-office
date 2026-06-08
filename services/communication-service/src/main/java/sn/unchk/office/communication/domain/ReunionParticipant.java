package sn.unchk.office.communication.domain;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.util.UUID;

/**
 * Participant à une réunion. Identifié par la clé composite (réunion, personne).
 */
@Entity
@Table(name = "reunion_participants")
public class ReunionParticipant {

    @EmbeddedId
    private ReunionParticipantId id;

    /** Nature du participant (personnel ou étudiant). */
    @Enumerated(EnumType.STRING)
    @Column(name = "person_kind", nullable = false)
    private PersonKind personKind;

    /** Émargement (présent / absent / inconnu). */
    @Column(name = "is_present")
    private Boolean isPresent;

    protected ReunionParticipant() {
        // Requis par JPA.
    }

    public ReunionParticipant(UUID reunionId, UUID personRef, PersonKind personKind) {
        this.id = new ReunionParticipantId(reunionId, personRef);
        this.personKind = personKind;
    }

    public ReunionParticipantId getId() {
        return id;
    }

    public UUID getReunionId() {
        return id != null ? id.getReunionId() : null;
    }

    public UUID getPersonRef() {
        return id != null ? id.getPersonRef() : null;
    }

    public PersonKind getPersonKind() {
        return personKind;
    }

    public void setPersonKind(PersonKind personKind) {
        this.personKind = personKind;
    }

    public Boolean getIsPresent() {
        return isPresent;
    }

    public void setIsPresent(Boolean isPresent) {
        this.isPresent = isPresent;
    }
}
