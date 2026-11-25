package lk.sliit.ieee.eventfinance.repository;

import java.util.*;
import jakarta.persistence.LockModeType;
import lk.sliit.ieee.eventfinance.entity.PurchaseGroup;
import org.springframework.data.jpa.repository.*;

public interface PurchaseGroupRepository extends JpaRepository<PurchaseGroup, Long> {
    List<PurchaseGroup> findByEventIdOrderByIdAsc(Long eventId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<PurchaseGroup> findByIdAndEventId(Long id, Long eventId);
    Optional<PurchaseGroup> findFirstByIdAndEventId(Long id,Long eventId);
    boolean existsByBudgetCategoryId(Long categoryId);
    @Modifying
    @Query("update PurchaseGroup g set g.owner=null where g.event.id=:eventId and g.owner.id=:userId")
    void clearOwnerForEvent(Long eventId, Long userId);
}
