package lk.sliit.ieee.eventfinance.service;

import java.math.BigDecimal;
import java.util.List;
import lk.sliit.ieee.eventfinance.dto.*;
import lk.sliit.ieee.eventfinance.entity.Event;
import lk.sliit.ieee.eventfinance.entity.Expense;
import lk.sliit.ieee.eventfinance.exception.EventNotFoundException;
import lk.sliit.ieee.eventfinance.repository.EventRepository;
import lk.sliit.ieee.eventfinance.repository.ExpenseRepository;
import org.springframework.stereotype.Service;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

@Service
@PreAuthorize("@access.finance(authentication,#eventId)")
@Transactional(readOnly = true)
public class ExpenseService {
    private final AccessPolicy access;
    private final EventRepository events;
    private final ExpenseRepository expenses;

    public ExpenseService(EventRepository events, ExpenseRepository expenses,AccessPolicy access) {
        this.access=access;
        this.events = events;
        this.expenses = expenses;
    }

    @Transactional
    public ExpenseResponse create(Long eventId, CreateExpenseRequest request) {
        Event event = findEvent(eventId);
        Expense expense = new Expense(event, request.description().strip(),
                request.category().strip(), request.amount(), request.expenseDate());
        expense.setCreatedBy(access.user());
        return toResponse(expenses.save(expense));
    }

    public EventExpensesResponse findByEvent(Long eventId) {
        Event event = findEvent(eventId);
        List<ExpenseResponse> records = expenses.findByEventIdOrderByExpenseDateDescIdDesc(eventId)
                .stream().filter(record -> access.owns(record.getCreatedById())).map(this::toResponse).toList();
        BigDecimal total = records.stream().map(ExpenseResponse::amount)
                .reduce(new BigDecimal("0.00"), BigDecimal::add);
        return new EventExpensesResponse(eventId, event.getPlannedBudget(), total,
                event.getPlannedBudget().subtract(total), records);
    }

    @Transactional
    public ExpenseResponse update(Long eventId,Long id,CreateExpenseRequest request) {
        var record=expenses.findByIdAndEventId(id,eventId).orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND,"Record not found."));
        if(!access.owns(record.getCreatedById())) throw new org.springframework.security.access.AccessDeniedException("Access denied");
        record.update(request.description(),request.category(),request.amount(),request.expenseDate());
        return toResponse(record);
    }
    private Event findEvent(Long eventId) {
        return events.findById(eventId).orElseThrow(() -> new EventNotFoundException(eventId));
    }

    private ExpenseResponse toResponse(Expense expense) {
        return new ExpenseResponse(expense.getId(), expense.getDescription(), expense.getCategory(),
                expense.getAmount(), expense.getExpenseDate());
    }
}
