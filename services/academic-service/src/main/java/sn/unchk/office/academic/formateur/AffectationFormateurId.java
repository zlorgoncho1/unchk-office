package sn.unchk.office.academic.formateur;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * Clé composite d'une affectation de formateur : (formation, formateur, module).
 * <p>
 * Reflète la PK composite {@code (formation_id, formateur_ref, module)} de la table
 * {@code formation_formateurs}. Un même formateur peut être affecté à une formation
 * pour plusieurs matières (modules) distinctes.
 */
@Embeddable
public class AffectationFormateurId implements Serializable {

    /** Formation concernée (UUID local). */
    @Column(name = "formation_id", nullable = false)
    private UUID formationId;

    /** Formateur affecté (référence logique people.staff.id). */
    @Column(name = "formateur_ref", nullable = false)
    private UUID formateurRef;

    /** Matière enseignée. */
    @Column(name = "module", nullable = false)
    private String module;

    protected AffectationFormateurId() {
        // Constructeur requis par JPA.
    }

    public AffectationFormateurId(UUID formationId, UUID formateurRef, String module) {
        this.formationId = formationId;
        this.formateurRef = formateurRef;
        this.module = module;
    }

    public UUID getFormationId() {
        return formationId;
    }

    public UUID getFormateurRef() {
        return formateurRef;
    }

    public String getModule() {
        return module;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AffectationFormateurId autre)) {
            return false;
        }
        return Objects.equals(formationId, autre.formationId)
                && Objects.equals(formateurRef, autre.formateurRef)
                && Objects.equals(module, autre.module);
    }

    @Override
    public int hashCode() {
        return Objects.hash(formationId, formateurRef, module);
    }
}
