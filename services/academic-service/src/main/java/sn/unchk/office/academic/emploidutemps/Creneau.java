package sn.unchk.office.academic.emploidutemps;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Créneau d'emploi du temps (table {@code schedule_slots}).
 * <p>
 * Un créneau appartient à une formation et décrit un cours : il est soit récurrent
 * (jour de la semaine), soit ponctuel (date précise). L'intervenant est désigné par
 * une référence logique vers {@code people.staff.id} (résolu via le read-model local).
 */
@Entity
@Table(name = "schedule_slots")
public class Creneau {

    /** Identifiant opaque du créneau (UUID). */
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /** Formation à laquelle se rattache le créneau. */
    @Column(name = "formation_id", nullable = false)
    private UUID formationId;

    /** Intitulé du cours / de la séance. */
    @Column(name = "course_label", nullable = false)
    private String courseLabel;

    /** Intervenant (référence logique people.staff.id), optionnel. */
    @Column(name = "formateur_ref")
    private UUID formateurRef;

    /** Jour de la semaine pour un créneau récurrent (exclusif avec sessionDate). */
    @Convert(converter = JourSemaineConverter.class)
    @Column(name = "day_of_week")
    private JourSemaine dayOfWeek;

    /** Date pour un créneau ponctuel (exclusif avec dayOfWeek). */
    @Column(name = "session_date")
    private LocalDate sessionDate;

    /** Heure de début. */
    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    /** Heure de fin (doit être > startTime). */
    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    /** Salle ou lien visio. */
    @Column(name = "room")
    private String room;

    /** Horodatage de création. */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** Horodatage de dernière modification. */
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Creneau() {
        // Constructeur requis par JPA.
    }

    /** Initialise l'identifiant et les horodatages avant la première persistance. */
    @PrePersist
    void avantInsertion() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        Instant maintenant = Instant.now();
        if (createdAt == null) {
            createdAt = maintenant;
        }
        updatedAt = maintenant;
    }

    /** Met à jour l'horodatage de modification avant chaque mise à jour. */
    @PreUpdate
    void avantMiseAJour() {
        updatedAt = Instant.now();
    }

    // --- Accesseurs / mutateurs ---

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getFormationId() {
        return formationId;
    }

    public void setFormationId(UUID formationId) {
        this.formationId = formationId;
    }

    public String getCourseLabel() {
        return courseLabel;
    }

    public void setCourseLabel(String courseLabel) {
        this.courseLabel = courseLabel;
    }

    public UUID getFormateurRef() {
        return formateurRef;
    }

    public void setFormateurRef(UUID formateurRef) {
        this.formateurRef = formateurRef;
    }

    public JourSemaine getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(JourSemaine dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public LocalDate getSessionDate() {
        return sessionDate;
    }

    public void setSessionDate(LocalDate sessionDate) {
        this.sessionDate = sessionDate;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    public String getRoom() {
        return room;
    }

    public void setRoom(String room) {
        this.room = room;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
