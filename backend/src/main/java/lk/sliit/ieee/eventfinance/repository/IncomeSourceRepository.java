package lk.sliit.ieee.eventfinance.repository;
import java.util.*;
import lk.sliit.ieee.eventfinance.entity.IncomeSource;
import org.springframework.data.jpa.repository.JpaRepository;
public interface IncomeSourceRepository extends JpaRepository<IncomeSource,Long> {
    boolean existsByEventIdAndNameKey(Long eventId,String nameKey);
    List<IncomeSource> findByEventIdOrderByNameAsc(Long eventId);
}
