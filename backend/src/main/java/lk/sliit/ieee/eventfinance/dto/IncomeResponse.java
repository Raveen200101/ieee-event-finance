package lk.sliit.ieee.eventfinance.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record IncomeResponse(Long id, String description, String source,
                              BigDecimal amount, LocalDate incomeDate) {}
