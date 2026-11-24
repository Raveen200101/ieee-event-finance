package lk.sliit.ieee.eventfinance.dto;
import jakarta.validation.constraints.*;
public record CreatePurchaseGroupRequest(@NotBlank @Size(max = 150) String name,
        @NotNull @Positive Long budgetCategoryId, @Size(max = 1000) String notes) {}
