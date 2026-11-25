package lk.sliit.ieee.eventfinance.dto;
import java.util.List;
import lk.sliit.ieee.eventfinance.entity.*;
public record ChapterAccountResponse(Long id, String email, String displayName,
        ChapterRole role, boolean enabled, boolean accountAdmin, List<CurrentEvent> currentEvents) {
    public record CurrentEvent(Long id, String name, String status) {}
    public static ChapterAccountResponse from(ChapterUser user, List<CurrentEvent> currentEvents) {
        return new ChapterAccountResponse(user.getId(),user.getEmail(),user.getDisplayName(),
                user.getRole(),user.isEnabled(),user.isAccountAdmin(),currentEvents);
    }
}
