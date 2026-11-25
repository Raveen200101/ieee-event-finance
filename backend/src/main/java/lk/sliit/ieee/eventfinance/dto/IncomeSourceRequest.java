package lk.sliit.ieee.eventfinance.dto;
import jakarta.validation.constraints.*;
public record IncomeSourceRequest(@NotBlank @Size(max=150) String name) {}
