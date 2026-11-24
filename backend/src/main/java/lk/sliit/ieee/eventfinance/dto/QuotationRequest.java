package lk.sliit.ieee.eventfinance.dto;
import java.math.BigDecimal;
import jakarta.validation.constraints.*;
public record QuotationRequest(@NotBlank @Size(max = 150) String supplier,
        @NotNull @Min(1) @Max(1000000) Integer quantity,
        @NotNull @DecimalMin("0.01") @Digits(integer = 13, fraction = 2) BigDecimal unitPrice,
        @Size(max = 1000) String notes) {}
