package lk.sliit.ieee.eventfinance.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateEventRequest(
    @NotBlank @Size(max = 150) String name,
    @NotNull @Size(max = 2000) String description,
    @NotNull LocalDate eventDate,
    @NotBlank @Size(max = 255) String venue,
    @NotNull @DecimalMin("0.00") @Digits(integer = 13, fraction = 2) BigDecimal plannedBudget
) {}
