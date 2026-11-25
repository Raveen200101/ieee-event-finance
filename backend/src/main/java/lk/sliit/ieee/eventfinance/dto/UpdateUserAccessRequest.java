package lk.sliit.ieee.eventfinance.dto;
import jakarta.validation.constraints.NotNull;
import lk.sliit.ieee.eventfinance.entity.ChapterRole;
public record UpdateUserAccessRequest(@NotNull ChapterRole role, @NotNull Boolean enabled, @NotNull Boolean accountAdmin) {}
