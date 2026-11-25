package lk.sliit.ieee.eventfinance.service;
import java.util.*;
import lk.sliit.ieee.eventfinance.entity.*;
import lk.sliit.ieee.eventfinance.repository.ChapterUserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
@Service("access")
public class AccessPolicy {
    private final ChapterUserRepository users;
    private final lk.sliit.ieee.eventfinance.repository.EventMembershipRepository memberships;
    private final lk.sliit.ieee.eventfinance.repository.PurchaseGroupRepository purchases;
    public AccessPolicy(ChapterUserRepository users,lk.sliit.ieee.eventfinance.repository.EventMembershipRepository memberships,lk.sliit.ieee.eventfinance.repository.PurchaseGroupRepository purchases) { this.users=users;this.memberships=memberships;this.purchases=purchases; }
    private Optional<ChapterUser> current(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) return Optional.empty();
        try { return users.findById(Long.valueOf(jwt.getSubject())).filter(ChapterUser::isEnabled)
                .filter(u -> u.getCredentialVersion() == ((Number) jwt.getClaim("ver")).longValue()); }
        catch (RuntimeException exception) { return Optional.empty(); }
    }
    public boolean committee(Authentication authentication) { return current(authentication).filter(u -> u.getRole() == ChapterRole.COMMITTEE_MEMBER).isPresent(); }
    public boolean admin(Authentication authentication) { return current(authentication).filter(u -> u.isAccountAdmin() && u.getRole() == ChapterRole.COMMITTEE_MEMBER).isPresent(); }
    public boolean self(Authentication authentication, Long id) { return current(authentication).filter(u -> u.getId().equals(id)).isPresent(); }
    public boolean event(Authentication auth,Long eventId) { return committee(auth) || current(auth).filter(u -> memberships.existsByEventIdAndUserId(eventId,u.getId())).isPresent(); }
    public boolean finance(Authentication auth,Long eventId) { return committee(auth) || current(auth).filter(u -> u.getRole()==ChapterRole.FINANCE_TEAM_LEAD && memberships.existsByEventIdAndUserId(eventId,u.getId())).isPresent(); }
    public boolean procurement(Authentication auth,Long eventId) { return committee(auth) || current(auth).filter(u -> u.getRole()==ChapterRole.LOGISTICS_TEAM_LEAD && memberships.existsByEventIdAndUserId(eventId,u.getId())).isPresent(); }
    public boolean purchase(Authentication auth,Long eventId,Long groupId) {
        return committee(auth) || procurement(auth,eventId) && purchases.findFirstByIdAndEventId(groupId,eventId).filter(g -> g.getOwnerId()!=null && g.getOwnerId().equals(user().getId())).isPresent();
    }
    public ChapterUser user() { return current(org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication()).orElseThrow(() -> new org.springframework.security.access.AccessDeniedException("Access denied")); }
    public boolean committee() { return committee(org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication()); }
    public boolean owns(Long creator) { return committee() || user().getId().equals(creator); }
}
