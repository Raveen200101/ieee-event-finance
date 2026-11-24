package lk.sliit.ieee.eventfinance.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateExpenseRequest(
        @NotBlank @Size(max = 500) String description,
        @NotBlank @Size(max = 50) String category,
        @NotNull @DecimalMin("0.01") @Digits(integer = 13, fraction = 2) BigDecimal amount,
        @NotNull LocalDate expenseDate
) {}
