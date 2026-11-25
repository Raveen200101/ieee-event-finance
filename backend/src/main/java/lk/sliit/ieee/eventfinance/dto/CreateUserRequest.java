package lk.sliit.ieee.eventfinance.dto;
import jakarta.validation.constraints.*;
import lk.sliit.ieee.eventfinance.entity.ChapterRole;
import java.util.List;
public record CreateUserRequest(@NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(max = 150) String displayName,
        @NotNull @Size(min = 12, max = 72) String password, @NotNull ChapterRole role,
        @Size(max = 500) List<@NotNull @Positive Long> eventIds) {
    public CreateUserRequest(String email, String displayName, String password, ChapterRole role) {
        this(email, displayName, password, role, List.of());
    }
}
