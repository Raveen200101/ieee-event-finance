package lk.sliit.ieee.eventfinance.repository;

import java.util.List;
import lk.sliit.ieee.eventfinance.entity.Income;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IncomeRepository extends JpaRepository<Income, Long> {
    java.util.Optional<Income> findByIdAndEventId(Long id,Long eventId);
    List<Income> findByEventIdOrderByIncomeDateDescIdDesc(Long eventId);
}
