package lk.sliit.ieee.eventfinance.repository;

import lk.sliit.ieee.eventfinance.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventRepository extends JpaRepository<Event, Long> {
    @org.springframework.data.jpa.repository.Query("select e from Event e where e.id in (select m.event.id from EventMembership m where m.user.id=:userId) order by e.eventDate desc,e.id desc")
    java.util.List<Event> findAssigned(Long userId);
}
