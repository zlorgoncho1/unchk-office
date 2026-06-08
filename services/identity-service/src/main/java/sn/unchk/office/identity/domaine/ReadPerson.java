package sn.unchk.office.identity.domaine;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Projection locale (read-model) d'une personne canonique du people-service.
 * <p>
 * Alimentée en lecture seule par les topics {@code people.students} et {@code people.staff}.
 * Permet à identity-service de vérifier qu'un {@code person_ref} référencé lors de la création
 * d'un compte correspond bien à une personne existante, SANS appel REST inter-service.
 */
@Entity
@Table(name = "read_person")
public class ReadPerson {

    /** Identifiant canonique de la personne (= people.students.id ou people.staff.id). */
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /** Nature de la personne : {@code etudiant} ou {@code personnel}. */
    @Column(name = "person_kind", nullable = false)
    private String personKind;

    @Column(name = "full_name")
    private String fullName;

    @Column(name = "email")
    private String email;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ReadPerson() {
        // Requis par JPA.
    }

    public ReadPerson(UUID id, String personKind) {
        this.id = id;
        this.personKind = personKind;
        this.updatedAt = Instant.now();
    }

    /** Met à jour les attributs projetés (upsert idempotent depuis Kafka). */
    public void mettreAJour(String fullName, String email) {
        this.fullName = fullName;
        this.email = email;
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getPersonKind() {
        return personKind;
    }

    public void setPersonKind(String personKind) {
        this.personKind = personKind;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
