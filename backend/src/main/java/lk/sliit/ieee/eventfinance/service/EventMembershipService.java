package lk.sliit.ieee.eventfinance.service;
import java.util.List;
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
    public EventMembershipService(EventRepository events,ChapterUserRepository users,EventMembershipRepository memberships) { this.events=events;this.users=users;this.memberships=memberships; }
    public List<Long> list(Long eventId) { return memberships.findByEventId(eventId).stream().map(EventMembership::getUserId).toList(); }
    public void assign(Long eventId,Long userId) {
        var event=events.findById(eventId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Event not found."));
        var user=users.lockById(userId).filter(u -> u.isEnabled()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Active user not found."));
        if(!memberships.existsByEventIdAndUserId(eventId,userId)) memberships.save(new EventMembership(event,user));
    }
    public void remove(Long eventId,Long userId) { memberships.remove(eventId,userId); }
}
