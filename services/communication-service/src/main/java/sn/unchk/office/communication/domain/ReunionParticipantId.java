package sn.unchk.office.communication.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * Clé composite de {@link ReunionParticipant} : (réunion, personne).
 */
@Embeddable
public class ReunionParticipantId implements Serializable {

    @Column(name = "reunion_id", nullable = false)
    private UUID reunionId;

    @Column(name = "person_ref", nullable = false)
    private UUID personRef;

    protected ReunionParticipantId() {
        // Requis par JPA.
    }

    public ReunionParticipantId(UUID reunionId, UUID personRef) {
        this.reunionId = reunionId;
        this.personRef = personRef;
    }

    public UUID getReunionId() {
        return reunionId;
    }

    public UUID getPersonRef() {
        return personRef;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ReunionParticipantId autre)) {
            return false;
        }
        return Objects.equals(reunionId, autre.reunionId)
                && Objects.equals(personRef, autre.personRef);
    }

    @Override
    public int hashCode() {
        return Objects.hash(reunionId, personRef);
    }
}
