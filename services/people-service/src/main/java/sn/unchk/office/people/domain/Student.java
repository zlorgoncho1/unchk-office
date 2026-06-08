package sn.unchk.office.people.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entite CANONIQUE Etudiant.
 * <p>
 * Source de verite de l'etudiant pour toute la plateforme. Reference par UUID
 * partout ailleurs (academic, insertion, communication, identity). Toute
 * modification declenche l'emission d'un evenement sur le topic {@code people.students}.
 * <p>
 * Cle primaire UUID (anti-enumeration / anti-IDOR), verrou optimiste {@code version},
 * suppression logique via {@code deletedAt}.
 */
@Entity
@Table(name = "students")
public class Student {

    /** Identifiant canonique opaque (UUID, genere cote base par defaut). */
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /** Identifiant National Etudiant (unique). */
    @Column(name = "ine", nullable = false, unique = true, length = 32)
    private String ine;

    /** Matricule interne UNCHK (unique, optionnel). */
    @Column(name = "matricule", unique = true, length = 32)
    private String matricule;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    /** Genre : mappe sur le type enumere natif PostgreSQL {@code genre}. */
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "gender", nullable = false, columnDefinition = "genre")
    private Genre gender;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(name = "birth_place")
    private String birthPlace;

    @Column(name = "email")
    private String email;

    @Column(name = "phone", length = 32)
    private String phone;

    @Column(name = "address")
    private String address;

    /** Reference de l'objet MinIO (bucket avatars). */
    @Column(name = "photo_object_key")
    private String photoObjectKey;

    /** Reference logique vers academic.formations.id (UUID, hors base). */
    @Column(name = "formation_ref")
    private UUID formationRef;

    @Column(name = "promotion", length = 32)
    private String promotion;

    @Column(name = "enrollment_year")
    private Short enrollmentYear;

    @Column(name = "exit_year")
    private Short exitYear;

    /**
     * Reference logique vers identity.users.id (compte de l'etudiant).
     * Sert a resoudre la fiche "me" cote serveur (anti-IDOR), jamais expose au client.
     */
    @Column(name = "user_ref")
    private UUID userRef;

    /** Statut : contrainte CHECK en base, stocke en texte. */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private StudentStatus status = StudentStatus.inscrit;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    /** Auteur de la creation (identity.users.id). */
    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Suppression logique : non nul si l'etudiant est archive. */
    @Column(name = "deleted_at")
    private Instant deletedAt;

    /** Diplomes obtenus (cascade : la suppression de l'etudiant supprime ses diplomes). */
    @OneToMany(mappedBy = "student", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("obtainedAt DESC")
    private List<StudentDiploma> diplomas = new ArrayList<>();

    /** Initialise l'UUID et les horodatages a la premiere persistance. */
    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        Instant maintenant = Instant.now();
        if (createdAt == null) {
            createdAt = maintenant;
        }
        if (updatedAt == null) {
            updatedAt = maintenant;
        }
        if (status == null) {
            status = StudentStatus.inscrit;
        }
    }

    // --- Accesseurs ---

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getIne() {
        return ine;
    }

    public void setIne(String ine) {
        this.ine = ine;
    }

    public String getMatricule() {
        return matricule;
    }

    public void setMatricule(String matricule) {
        this.matricule = matricule;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public Genre getGender() {
        return gender;
    }

    public void setGender(Genre gender) {
        this.gender = gender;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    public String getBirthPlace() {
        return birthPlace;
    }

    public void setBirthPlace(String birthPlace) {
        this.birthPlace = birthPlace;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPhotoObjectKey() {
        return photoObjectKey;
    }

    public void setPhotoObjectKey(String photoObjectKey) {
        this.photoObjectKey = photoObjectKey;
    }

    public UUID getFormationRef() {
        return formationRef;
    }

    public void setFormationRef(UUID formationRef) {
        this.formationRef = formationRef;
    }

    public String getPromotion() {
        return promotion;
    }

    public void setPromotion(String promotion) {
        this.promotion = promotion;
    }

    public Short getEnrollmentYear() {
        return enrollmentYear;
    }

    public void setEnrollmentYear(Short enrollmentYear) {
        this.enrollmentYear = enrollmentYear;
    }

    public Short getExitYear() {
        return exitYear;
    }

    public void setExitYear(Short exitYear) {
        this.exitYear = exitYear;
    }

    public UUID getUserRef() {
        return userRef;
    }

    public void setUserRef(UUID userRef) {
        this.userRef = userRef;
    }

    public StudentStatus getStatus() {
        return status;
    }

    public void setStatus(StudentStatus status) {
        this.status = status;
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(UUID createdBy) {
        this.createdBy = createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(Instant deletedAt) {
        this.deletedAt = deletedAt;
    }

    public List<StudentDiploma> getDiplomas() {
        return diplomas;
    }

    public void setDiplomas(List<StudentDiploma> diplomas) {
        this.diplomas = diplomas;
    }

    /** Ajoute un diplome en maintenant le lien bidirectionnel. */
    public void ajouterDiplome(StudentDiploma diplome) {
        diplome.setStudent(this);
        this.diplomas.add(diplome);
    }
}
