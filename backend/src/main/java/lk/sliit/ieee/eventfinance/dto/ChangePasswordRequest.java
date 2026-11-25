package lk.sliit.ieee.eventfinance.dto;
import jakarta.validation.constraints.*;
public record ChangePasswordRequest(@NotBlank @Size(max = 72) String currentPassword,
        @NotNull @Size(min = 12, max = 72) String newPassword) {}
