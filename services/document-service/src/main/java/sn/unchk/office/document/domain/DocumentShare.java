package sn.unchk.office.document.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Partage nominatif d'un document avec un utilisateur précis (par UUID).
 * Permet d'accorder l'accès (et éventuellement l'édition) hors visibilité par rôle.
 */
@Entity
@Table(name = "document_shares")
public class DocumentShare {

    @EmbeddedId
    private Cle cle;

    @Column(name = "can_edit", nullable = false)
    private boolean canEdit;

    @Column(name = "granted_at", nullable = false)
    private Instant grantedAt;

    protected DocumentShare() {
        // Constructeur requis par JPA.
    }

    public DocumentShare(UUID documentId, UUID userId, boolean canEdit) {
        this.cle = new Cle(documentId, userId);
        this.canEdit = canEdit;
    }

    @PrePersist
    void avantCreation() {
        if (grantedAt == null) {
            grantedAt = Instant.now();
        }
    }

    public UUID getDocumentId() {
        return cle.getDocumentId();
    }

    public UUID getUserId() {
        return cle.getUserId();
    }

    public boolean isCanEdit() {
        return canEdit;
    }

    public Instant getGrantedAt() {
        return grantedAt;
    }

    /** Clé composite (document_id, user_id). */
    @Embeddable
    public static class Cle implements Serializable {

        @Column(name = "document_id", nullable = false)
        private UUID documentId;

        @Column(name = "user_id", nullable = false)
        private UUID userId;

        protected Cle() {
        }

        public Cle(UUID documentId, UUID userId) {
            this.documentId = documentId;
            this.userId = userId;
        }

        public UUID getDocumentId() {
            return documentId;
        }

        public UUID getUserId() {
            return userId;
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
                    && Objects.equals(userId, autre.userId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(documentId, userId);
        }
    }
}
