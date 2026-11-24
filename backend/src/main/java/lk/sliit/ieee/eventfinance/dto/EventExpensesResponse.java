package lk.sliit.ieee.eventfinance.dto;

import java.math.BigDecimal;
import java.util.List;

public record EventExpensesResponse(Long eventId, BigDecimal plannedBudget,
                                   BigDecimal totalSpent, BigDecimal remainingBudget,
                                   List<ExpenseResponse> expenses) {}
