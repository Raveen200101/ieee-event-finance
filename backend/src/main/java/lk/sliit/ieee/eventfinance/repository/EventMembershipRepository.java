package lk.sliit.ieee.eventfinance.repository;
import java.util.List;
import lk.sliit.ieee.eventfinance.entity.EventMembership;
import org.springframework.data.jpa.repository.*;
public interface EventMembershipRepository extends JpaRepository<EventMembership,Long> {
    @Query("select (count(m)>0) from EventMembership m where m.event.id=:eventId and m.user.id=:userId")
    boolean existsByEventIdAndUserId(Long eventId,Long userId);
    List<EventMembership> findByEventId(Long eventId);
    @Modifying @Query("delete from EventMembership m where m.event.id=:eventId and m.user.id=:userId") void remove(Long eventId,Long userId);
}
