package sn.unchk.office.document.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * Visibilité d'un document par rôle (ABAC). L'ensemble des rôles déclarés constitue
 * le {@code visibility[]} transmis à OPA pour la décision d'accès au niveau objet.
 */
@Entity
@Table(name = "document_visibility")
public class DocumentVisibility {

    @EmbeddedId
    private Cle cle;

    protected DocumentVisibility() {
        // Constructeur requis par JPA.
    }

    public DocumentVisibility(UUID documentId, String role) {
        this.cle = new Cle(documentId, role);
    }

    public UUID getDocumentId() {
        return cle.getDocumentId();
    }

    public String getRole() {
        return cle.getRole();
    }

    /** Clé composite (document_id, role). */
    @Embeddable
    public static class Cle implements Serializable {

        @Column(name = "document_id", nullable = false)
        private UUID documentId;

        @Column(name = "role", nullable = false)
        private String role;

        protected Cle() {
        }

        public Cle(UUID documentId, String role) {
            this.documentId = documentId;
            this.role = role;
        }

        public UUID getDocumentId() {
            return documentId;
        }

        public String getRole() {
            return role;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Cle autre)) {
                return false;
            }
            return Objects.equals(documentId, autre.documentId)
                    && Objects.equals(role, autre.role);
        }

        @Override
        public int hashCode() {
            return Objects.hash(documentId, role);
        }
    }
}
