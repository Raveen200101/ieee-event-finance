package lk.sliit.ieee.eventfinance.service;
import java.util.List;
import java.util.HashSet;
import lk.sliit.ieee.eventfinance.dto.EventMemberResponse;
import lk.sliit.ieee.eventfinance.entity.ChapterRole;
import lk.sliit.ieee.eventfinance.entity.EventMembership;
import lk.sliit.ieee.eventfinance.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
@Service @Transactional @PreAuthorize("@access.committee(authentication)")
public class EventMembershipService {
    private final EventRepository events; private final ChapterUserRepository users; private final EventMembershipRepository memberships;
    private final PurchaseGroupRepository purchases;
    public EventMembershipService(EventRepository events,ChapterUserRepository users,EventMembershipRepository memberships,PurchaseGroupRepository purchases) { this.events=events;this.users=users;this.memberships=memberships;this.purchases=purchases; }
    private void requireEvent(Long eventId) {
        if (!events.existsById(eventId)) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Event not found.");
    }
    public List<Long> list(Long eventId) { requireEvent(eventId); return memberships.findByEventId(eventId).stream().map(EventMembership::getUserId).toList(); }
    @Transactional(readOnly=true)
    public List<EventMemberResponse> options(Long eventId) {
        var assigned=new HashSet<>(list(eventId));
        return users.findAllByOrderByIdAsc().stream()
                .filter(u -> u.getRole()!=ChapterRole.COMMITTEE_MEMBER || assigned.contains(u.getId()))
                .filter(u -> u.isEnabled() || assigned.contains(u.getId()))
                .map(u -> new EventMemberResponse(u.getId(),u.getDisplayName(),u.getRole(),u.isEnabled(),assigned.contains(u.getId())))
                .toList();
    }
    public void assign(Long eventId,Long userId) {
        var event=events.findById(eventId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Event not found."));
        var user=users.lockById(userId).filter(u -> u.isEnabled()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Active user not found."));
        if(user.getRole()==ChapterRole.COMMITTEE_MEMBER) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Committee members already have access to every event.");
        if(!memberships.existsByEventIdAndUserId(eventId,userId)) memberships.save(new EventMembership(event,user));
    }
    public void remove(Long eventId,Long userId) {
        requireEvent(eventId);
        users.lockById(userId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"User not found."));
        purchases.clearOwnerForEvent(eventId,userId);
        memberships.remove(eventId,userId);
    }
}
