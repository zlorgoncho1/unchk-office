package sn.unchk.office.insertion.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Entrée du registre de contact (suivi du devenir des diplômés).
 * <p>
 * Trace un échange avec un étudiant : date, canal, notes et agent d'appui à l'insertion.
 * Table en simple journal (pas de verrou optimiste ni de suppression logique).
 */
@Entity
@Table(name = "contact_log")
public class ContactLog {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /** Étudiant contacté → people.students.id (réf logique). */
    @Column(name = "student_ref", nullable = false)
    private UUID studentRef;

    @Column(name = "contacted_at", nullable = false)
    private LocalDate contactedAt = LocalDate.now();

    /** Canal de contact : téléphone, email, présentiel... */
    @Column(name = "channel")
    private String channel;

    @Column(name = "notes")
    private String notes;

    /** Agent d'appui à l'insertion → people.staff.id (réf logique). */
    @Column(name = "agent_ref")
    private UUID agentRef;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected ContactLog() {
        // Requis par JPA.
    }

    @PrePersist
    void avantCreation() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (contactedAt == null) {
            contactedAt = LocalDate.now();
        }
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getStudentRef() {
        return studentRef;
    }

    public void setStudentRef(UUID studentRef) {
        this.studentRef = studentRef;
    }

    public LocalDate getContactedAt() {
        return contactedAt;
    }

    public void setContactedAt(LocalDate contactedAt) {
        this.contactedAt = contactedAt;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public UUID getAgentRef() {
        return agentRef;
    }

    public void setAgentRef(UUID agentRef) {
        this.agentRef = agentRef;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
