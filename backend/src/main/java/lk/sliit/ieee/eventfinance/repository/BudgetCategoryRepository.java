package lk.sliit.ieee.eventfinance.repository;

import java.util.List;
import java.util.Optional;
import lk.sliit.ieee.eventfinance.entity.BudgetCategory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BudgetCategoryRepository extends JpaRepository<BudgetCategory, Long> {
    List<BudgetCategory> findByEventIdOrderByCategoryAscIdAsc(Long eventId);
    Optional<BudgetCategory> findByIdAndEventId(Long id, Long eventId);
    boolean existsByEventIdAndCategoryKey(Long eventId, String categoryKey);
}
