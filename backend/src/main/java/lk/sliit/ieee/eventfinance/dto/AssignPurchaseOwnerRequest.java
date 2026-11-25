package lk.sliit.ieee.eventfinance.dto;
import jakarta.validation.constraints.*;
public record AssignPurchaseOwnerRequest(@NotNull @Positive Long userId) {}
