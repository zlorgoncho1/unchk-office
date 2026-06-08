package sn.unchk.office.communication.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sn.unchk.office.communication.domain.ReunionParticipant;
import sn.unchk.office.communication.domain.ReunionParticipantId;

import java.util.List;
import java.util.UUID;

/**
 * Accès aux participants d'une réunion.
 */
public interface ReunionParticipantRepository
        extends JpaRepository<ReunionParticipant, ReunionParticipantId> {

    /** Participants d'une réunion donnée. */
    List<ReunionParticipant> findByIdReunionId(UUID reunionId);

    /** Supprime tous les participants d'une réunion (réaffectation complète). */
    void deleteByIdReunionId(UUID reunionId);
}
