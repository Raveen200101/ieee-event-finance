package lk.sliit.ieee.eventfinance.service;

import java.math.BigDecimal;
import java.util.List;
import lk.sliit.ieee.eventfinance.dto.*;
import lk.sliit.ieee.eventfinance.entity.Event;
import lk.sliit.ieee.eventfinance.entity.Income;
import lk.sliit.ieee.eventfinance.exception.EventNotFoundException;
import lk.sliit.ieee.eventfinance.repository.EventRepository;
import lk.sliit.ieee.eventfinance.repository.IncomeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class IncomeService {
    private final EventRepository events;
    private final IncomeRepository incomes;
    private final ExpenseService expenses;

    public IncomeService(EventRepository events, IncomeRepository incomes, ExpenseService expenses) {
        this.events = events;
        this.incomes = incomes;
        this.expenses = expenses;
    }

    @Transactional
    public IncomeResponse create(Long eventId, CreateIncomeRequest request) {
        Event event = events.findById(eventId).orElseThrow(() -> new EventNotFoundException(eventId));
        Income income = new Income(event, request.description().strip(),
                request.source().strip(), request.amount(), request.incomeDate());
        return toResponse(incomes.save(income));
    }

    public EventIncomesResponse findByEvent(Long eventId) {
        BigDecimal totalExpenses = expenses.findByEvent(eventId).totalSpent();
        List<IncomeResponse> records = incomes.findByEventIdOrderByIncomeDateDescIdDesc(eventId)
                .stream().map(this::toResponse).toList();
        BigDecimal totalIncome = records.stream().map(IncomeResponse::amount)
                .reduce(new BigDecimal("0.00"), BigDecimal::add);
        return new EventIncomesResponse(eventId, totalIncome, totalExpenses,
                totalIncome.subtract(totalExpenses), records);
    }

    private IncomeResponse toResponse(Income income) {
        return new IncomeResponse(income.getId(), income.getDescription(), income.getSource(),
                income.getAmount(), income.getIncomeDate());
    }
}
