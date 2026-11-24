package lk.sliit.ieee.eventfinance.repository;

import java.util.List;
import lk.sliit.ieee.eventfinance.entity.Expense;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    List<Expense> findByEventIdOrderByExpenseDateDescIdDesc(Long eventId);
}
