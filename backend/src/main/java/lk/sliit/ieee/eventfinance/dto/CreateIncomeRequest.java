package lk.sliit.ieee.eventfinance.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateIncomeRequest(
        @NotBlank @Size(max = 500) String description,
        @NotBlank @Size(max = 150) String source,
        @NotNull @DecimalMin("0.01") @Digits(integer = 13, fraction = 2) BigDecimal amount,
        @NotNull LocalDate incomeDate
) {}
