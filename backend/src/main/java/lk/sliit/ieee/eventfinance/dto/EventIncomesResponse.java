package lk.sliit.ieee.eventfinance.dto;

import java.math.BigDecimal;
import java.util.List;

public record EventIncomesResponse(Long eventId, BigDecimal totalIncome,
                                  BigDecimal totalExpenses, BigDecimal balance,
                                  List<IncomeResponse> incomes) {}
