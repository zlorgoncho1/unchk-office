package sn.unchk.office.academic.formation;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Agrégat racine du domaine : une formation de l'université.
 * <p>
 * Source de vérité locale (base {@code academic}). Toute évolution est publiée sur le topic
 * {@code academic.formations} pour alimenter les projections des autres services.
 * Clé primaire UUID (anti-énumération / anti-IDOR).
 */
@Entity
@Table(name = "formations")
public class Formation {

    /** Identifiant opaque de la formation (UUID, généré en base par défaut). */
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /** Code unique de la formation (ex : « LIC-INFO-01 »). */
    @Column(name = "code", length = 32, unique = true)
    private String code;

    /** Intitulé lisible de la formation. */
    @Column(name = "label", nullable = false)
    private String label;

    /** Niveau (certificat, licence, master...). */
    @Convert(converter = NiveauFormationConverter.class)
    @Column(name = "level", nullable = false)
    private NiveauFormation level;

    /** Type (initiale, continue, professionnelle...). */
    @Convert(converter = TypeFormationConverter.class)
    @Column(name = "kind", nullable = false)
    private TypeFormation kind = TypeFormation.INITIALE;

    /** Source de financement (peut être nulle si non renseignée). */
    @Convert(converter = FinancementConverter.class)
    @Column(name = "funding")
    private Financement funding;

    /** Date de début (incluse), optionnelle. */
    @Column(name = "start_date")
    private LocalDate startDate;

    /** Date de fin (incluse), optionnelle. Doit être >= startDate si les deux sont présentes. */
    @Column(name = "end_date")
    private LocalDate endDate;

    /** Nombre de formés de genre masculin (statistiques de genre). */
    @Column(name = "trained_male", nullable = false)
    private int trainedMale = 0;

    /** Nombre de formés de genre féminin (statistiques de genre). */
    @Column(name = "trained_female", nullable = false)
    private int trainedFemale = 0;

    /** Référence logique vers le responsable (people.staff.id), hors base. */
    @Column(name = "responsible_ref")
    private UUID responsibleRef;

    /** Formation active (visible / exploitable). */
    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    /** Verrou optimiste. */
    @Version
    @Column(name = "version", nullable = false)
    private long version;

    /** Auteur de la création (identity.users.id), pour l'audit minimal. */
    @Column(name = "created_by", nullable = false, updatable = false)
    private UUID createdBy;

    /** Horodatage de création. */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** Horodatage de dernière modification. */
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Suppression logique (NULL = active). */
    @Column(name = "deleted_at")
    private Instant deletedAt;

    protected Formation() {
        // Constructeur requis par JPA.
    }

    /**
     * Initialise l'identifiant et les horodatages avant la première persistance.
     */
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

    /**
     * Met à jour l'horodatage de modification avant chaque mise à jour.
     */
    @PreUpdate
    void avantMiseAJour() {
        updatedAt = Instant.now();
    }

    /** Indique si la formation est supprimée logiquement. */
    public boolean estSupprimee() {
        return deletedAt != null;
    }

    /** Marque la formation comme supprimée logiquement (et inactive). */
    public void supprimerLogiquement() {
        this.deletedAt = Instant.now();
        this.active = false;
    }

    // --- Accesseurs / mutateurs ---

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public NiveauFormation getLevel() {
        return level;
    }

    public void setLevel(NiveauFormation level) {
        this.level = level;
    }

    public TypeFormation getKind() {
        return kind;
    }

    public void setKind(TypeFormation kind) {
        this.kind = kind;
    }

    public Financement getFunding() {
        return funding;
    }

    public void setFunding(Financement funding) {
        this.funding = funding;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public int getTrainedMale() {
        return trainedMale;
    }

    public void setTrainedMale(int trainedMale) {
        this.trainedMale = trainedMale;
    }

    public int getTrainedFemale() {
        return trainedFemale;
    }

    public void setTrainedFemale(int trainedFemale) {
        this.trainedFemale = trainedFemale;
    }

    public UUID getResponsibleRef() {
        return responsibleRef;
    }

    public void setResponsibleRef(UUID responsibleRef) {
        this.responsibleRef = responsibleRef;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public long getVersion() {
        return version;
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

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }
}
