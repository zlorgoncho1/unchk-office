package sn.unchk.office.admin.mapper;

import org.springframework.stereotype.Component;
import sn.unchk.office.admin.domain.Budget;
import sn.unchk.office.admin.domain.BudgetLine;
import sn.unchk.office.admin.dto.BudgetDto;
import sn.unchk.office.admin.dto.BudgetResumeDto;
import sn.unchk.office.admin.dto.LigneBudgetaireDto;
import sn.unchk.office.admin.messaging.BudgetEventPayload;

import java.math.BigDecimal;
import java.util.List;

/**
 * Transforme les entités budgétaires en DTO de réponse et en charge utile d'événement.
 * <p>
 * Mapping explicite (jamais l'entité JPA exposée directement). Les écarts prévu/réalisé
 * sont calculés ici pour rester cohérents partout.
 */
@Component
public class BudgetMapper {

    /** Construit la vue détaillée d'un budget à partir de ses lignes. */
    public BudgetDto versDto(Budget budget, List<BudgetLine> lignes) {
        List<LigneBudgetaireDto> lignesDto = lignes.stream().map(this::versLigneDto).toList();
        BigDecimal ecartGlobal = budget.getTotalPlanned().subtract(budget.getTotalRealized());
        return new BudgetDto(
                budget.getId(),
                budget.getFiscalYear(),
                budget.getLabel(),
                budget.getStatus(),
                budget.getOrientationNote(),
                budget.getTotalPlanned(),
                budget.getTotalRealized(),
                ecartGlobal,
                budget.getCurrency(),
                budget.getCreatedAt(),
                budget.getUpdatedAt(),
                lignesDto);
    }

    /** Construit la vue résumée d'un budget (liste). */
    public BudgetResumeDto versResumeDto(Budget budget) {
        return new BudgetResumeDto(
                budget.getId(),
                budget.getFiscalYear(),
                budget.getLabel(),
                budget.getStatus(),
                budget.getTotalPlanned(),
                budget.getTotalRealized(),
                budget.getCurrency());
    }

    /** Construit la vue d'une ligne budgétaire avec son écart. */
    public LigneBudgetaireDto versLigneDto(BudgetLine ligne) {
        BigDecimal ecart = ligne.getPlannedAmount().subtract(ligne.getRealizedAmount());
        return new LigneBudgetaireDto(
                ligne.getId(),
                ligne.getCategory(),
                ligne.getDirection(),
                ligne.getPlannedAmount(),
                ligne.getRealizedAmount(),
                ecart,
                ligne.getLabel());
    }

    /** Construit la charge utile à publier sur Kafka (state transfer). */
    public BudgetEventPayload versPayload(Budget budget) {
        return new BudgetEventPayload(
                budget.getId(),
                budget.getFiscalYear(),
                budget.getLabel(),
                budget.getStatus(),
                budget.getTotalPlanned(),
                budget.getTotalRealized(),
                budget.getCurrency());
    }
}
