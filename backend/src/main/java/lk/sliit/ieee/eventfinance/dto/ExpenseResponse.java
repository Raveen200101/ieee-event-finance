package lk.sliit.ieee.eventfinance.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ExpenseResponse(Long id, String description, String category,
                              BigDecimal amount, LocalDate expenseDate) {}
