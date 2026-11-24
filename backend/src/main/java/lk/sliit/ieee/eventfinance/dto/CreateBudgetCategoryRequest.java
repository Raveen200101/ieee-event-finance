package lk.sliit.ieee.eventfinance.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record CreateBudgetCategoryRequest(
        @NotBlank @Size(max = 50) String category,
        @NotBlank @Size(max = 150) String owner,
        @NotNull @DecimalMin("0.00") @Digits(integer = 13, fraction = 2) BigDecimal plannedAmount) {}
