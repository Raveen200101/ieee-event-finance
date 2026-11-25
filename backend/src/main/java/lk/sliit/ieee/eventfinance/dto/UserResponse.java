package lk.sliit.ieee.eventfinance.dto;
import lk.sliit.ieee.eventfinance.entity.*;
public record UserResponse(Long id, String email, String displayName, ChapterRole role, boolean enabled, boolean accountAdmin) {
    public static UserResponse from(ChapterUser user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getDisplayName(), user.getRole(), user.isEnabled(), user.isAccountAdmin());
    }
}
