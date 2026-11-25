package lk.sliit.ieee.eventfinance.dto;
import jakarta.validation.constraints.*;
import lk.sliit.ieee.eventfinance.entity.ChapterRole;
public record CreateUserRequest(@NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(max = 150) String displayName,
        @NotNull @Size(min = 12, max = 72) String password, @NotNull ChapterRole role) {}
