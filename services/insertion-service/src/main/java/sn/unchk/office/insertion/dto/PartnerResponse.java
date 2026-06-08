package sn.unchk.office.insertion.dto;

import sn.unchk.office.insertion.domain.Partner;
import sn.unchk.office.insertion.domain.PartnerKind;

import java.util.UUID;

/**
 * Représentation d'un partenaire renvoyée au client.
 */
public record PartnerResponse(
        UUID id,
        String name,
        PartnerKind kind,
        String sector,
        String contactName,
        String contactEmail,
        String contactPhone,
        String address,
        String city,
        boolean active
) {

    /** Convertit l'entité en réponse (mapping explicite, pas d'exposition d'entité JPA). */
    public static PartnerResponse depuis(Partner p) {
        return new PartnerResponse(
                p.getId(),
                p.getName(),
                p.getKind(),
                p.getSector(),
                p.getContactName(),
                p.getContactEmail(),
                p.getContactPhone(),
                p.getAddress(),
                p.getCity(),
                p.isActive());
    }
}
