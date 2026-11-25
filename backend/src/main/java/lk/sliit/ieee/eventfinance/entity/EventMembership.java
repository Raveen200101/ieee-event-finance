package lk.sliit.ieee.eventfinance.entity;
import jakarta.persistence.*;
@Entity @Table(name="event_memberships")
public class EventMembership {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="event_id") private Event event;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="user_id") private ChapterUser user;
    protected EventMembership() {}
    public EventMembership(Event event,ChapterUser user) { this.event=event; this.user=user; }
    public Long getUserId() { return user.getId(); }
    public Event getEvent() { return event; }
}
