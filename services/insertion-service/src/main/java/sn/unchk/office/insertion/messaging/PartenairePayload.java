package sn.unchk.office.insertion.messaging;

import sn.unchk.office.insertion.domain.Partner;

import java.util.UUID;

/**
 * Charge utile d'un événement « partenaire » publié sur {@code insertion.events}.
 * <p>
 * Ne contient que l'état métier (pas de secret). Sérialisée en JSON dans l'enveloppe.
 */
public record PartenairePayload(
        UUID id,
        String name,
        String kind,
        String sector,
        String city,
        boolean active
) {

    public static PartenairePayload depuis(Partner p) {
        return new PartenairePayload(
                p.getId(),
                p.getName(),
                p.getKind() != null ? p.getKind().name() : null,
                p.getSector(),
                p.getCity(),
                p.isActive());
    }
}
