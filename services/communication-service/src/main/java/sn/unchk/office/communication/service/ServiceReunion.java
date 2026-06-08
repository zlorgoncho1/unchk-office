package sn.unchk.office.communication.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.unchk.office.common.messaging.Topics;
import sn.unchk.office.communication.domain.Reunion;
import sn.unchk.office.communication.domain.ReunionParticipant;
import sn.unchk.office.communication.dto.ParticipantDto;
import sn.unchk.office.communication.dto.ReunionCreationRequest;
import sn.unchk.office.communication.dto.ReunionDto;
import sn.unchk.office.communication.messaging.payload.ReunionEvent;
import sn.unchk.office.communication.messaging.producer.EnregistreurEvenement;
import sn.unchk.office.communication.projection.PeopleStaffRo;
import sn.unchk.office.communication.repository.PeopleStaffRoRepository;
import sn.unchk.office.communication.repository.ReunionParticipantRepository;
import sn.unchk.office.communication.repository.ReunionRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Cas d'usage des réunions : planification, consultation, liste.
 * <p>
 * Toute modification d'agrégat met en file un événement sur {@code communication.reunions}
 * (via l'Outbox), source des read-models des autres services et des convocations.
 */
@Service
public class ServiceReunion {

    private final ReunionRepository reunionRepository;
    private final ReunionParticipantRepository participantRepository;
    private final PeopleStaffRoRepository staffRo;
    private final EnregistreurEvenement enregistreur;

    public ServiceReunion(ReunionRepository reunionRepository,
                          ReunionParticipantRepository participantRepository,
                          PeopleStaffRoRepository staffRo,
                          EnregistreurEvenement enregistreur) {
        this.reunionRepository = reunionRepository;
        this.participantRepository = participantRepository;
        this.staffRo = staffRo;
        this.enregistreur = enregistreur;
    }

    /**
     * Planifie une réunion et enregistre ses participants.
     *
     * @param requete   données de la réunion
     * @param createurId identifiant de l'utilisateur courant (propriétaire / auteur)
     * @return la réunion créée
     */
    @Transactional
    public ReunionDto planifier(ReunionCreationRequest requete, UUID createurId) {
        // Contrôle de cohérence métier (en plus de la contrainte CHECK en base).
        if (requete.endsAt() != null && requete.endsAt().isBefore(requete.startsAt())) {
            throw new IllegalArgumentException("La fin doit être postérieure ou égale au début.");
        }

        Reunion reunion = new Reunion();
        reunion.setTitle(requete.title());
        reunion.setType(requete.type());
        reunion.setDescription(requete.description());
        reunion.setLocation(requete.location());
        reunion.setStartsAt(requete.startsAt());
        reunion.setEndsAt(requete.endsAt());
        reunion.setOrganizerId(requete.organizerId());
        reunion.setFormationRef(requete.formationRef());
        reunion.setCreatedBy(createurId);
        reunion = reunionRepository.save(reunion);

        List<UUID> participantIds = enregistrerParticipants(reunion.getId(), requete.participants());

        // Émission de l'événement (état de la réunion + participants pour la convocation).
        ReunionEvent event = ReunionEvent.de(reunion, participantIds);
        enregistreur.enregistrer("Reunion", reunion.getId(), Topics.COMMUNICATION_REUNIONS,
                "ReunionPlanifiee", event);

        return versDto(reunion);
    }

    /** Liste les réunions non supprimées (les plus récentes d'abord). */
    @Transactional(readOnly = true)
    public List<ReunionDto> lister() {
        return reunionRepository.findByDeletedAtIsNullOrderByStartsAtDesc()
                .stream()
                .map(this::versDto)
                .toList();
    }

    /**
     * Consulte une réunion par identifiant.
     *
     * @throws RessourceIntrouvableException si elle n'existe pas
     */
    @Transactional(readOnly = true)
    public ReunionDto consulter(UUID id) {
        Reunion reunion = reunionRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Réunion introuvable."));
        return versDto(reunion);
    }

    /** Persiste les participants d'une réunion et retourne leurs identifiants. */
    private List<UUID> enregistrerParticipants(UUID reunionId, List<ParticipantDto> participants) {
        List<UUID> ids = new ArrayList<>();
        if (participants == null) {
            return ids;
        }
        for (ParticipantDto p : participants) {
            ReunionParticipant participant =
                    new ReunionParticipant(reunionId, p.personRef(), p.personKind());
            participant.setIsPresent(p.isPresent());
            participantRepository.save(participant);
            ids.add(p.personRef());
        }
        return ids;
    }

    /** Construit le DTO en enrichissant le nom de l'organisateur depuis le read-model local. */
    private ReunionDto versDto(Reunion reunion) {
        String organizerName = staffRo.findById(reunion.getOrganizerId())
                .map(PeopleStaffRo::getFullName)
                .orElse(null);
        List<ParticipantDto> participants = participantRepository.findByIdReunionId(reunion.getId())
                .stream()
                .map(p -> new ParticipantDto(p.getPersonRef(), p.getPersonKind(), p.getIsPresent()))
                .toList();
        return ReunionDto.de(reunion, organizerName, participants);
    }
}
