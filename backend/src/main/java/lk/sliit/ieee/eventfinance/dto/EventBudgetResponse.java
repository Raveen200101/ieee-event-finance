package lk.sliit.ieee.eventfinance.dto;

import java.math.BigDecimal;
import java.util.List;

public record EventBudgetResponse(Long eventId, BigDecimal plannedBudget, BigDecimal totalAllocated,
        BigDecimal unallocatedBudget, BigDecimal totalActual, List<Category> categories) {
    public record Category(Long id, String category, String owner, BigDecimal plannedAmount,
            BigDecimal actualAmount, BigDecimal remainingAmount) {}
}
