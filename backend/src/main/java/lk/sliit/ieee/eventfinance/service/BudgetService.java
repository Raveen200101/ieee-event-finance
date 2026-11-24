package lk.sliit.ieee.eventfinance.service;

import java.math.BigDecimal;
import java.util.*;
import lk.sliit.ieee.eventfinance.dto.*;
import lk.sliit.ieee.eventfinance.entity.*;
import lk.sliit.ieee.eventfinance.exception.EventNotFoundException;
import lk.sliit.ieee.eventfinance.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class BudgetService {
    private final EventRepository events;
    private final BudgetCategoryRepository budgets;
    private final ExpenseRepository expenses;
    private final PurchaseGroupRepository purchases;

    public BudgetService(EventRepository events, BudgetCategoryRepository budgets, ExpenseRepository expenses, PurchaseGroupRepository purchases) {
        this.events = events;
        this.budgets = budgets;
        this.expenses = expenses;
        this.purchases = purchases;
    }
    private Event event(Long id) {
        return events.findById(id).orElseThrow(() -> new EventNotFoundException(id));
    }
    private BudgetCategory category(Long eventId, Long id) {
        event(eventId);
        return budgets.findByIdAndEventId(id, eventId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Budget category not found for this event."));
    }
    @Transactional
    public EventBudgetResponse create(Long eventId, CreateBudgetCategoryRequest request) {
        Event event = event(eventId);
        if (budgets.existsByEventIdAndCategoryKey(eventId, BudgetCategory.key(request.category()))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This category already has a budget. Edit its allocation instead.");
        }
        budgets.saveAndFlush(new BudgetCategory(event, request.category(), request.owner(), request.plannedAmount()));
        return findByEvent(eventId);
    }
    @Transactional
    public EventBudgetResponse update(Long eventId, Long id, UpdateBudgetCategoryRequest request) {
        category(eventId, id).update(request.owner(), request.plannedAmount());
        return findByEvent(eventId);
    }
    @Transactional
    public EventBudgetResponse delete(Long eventId, Long id) {
        var category = category(eventId, id);
        if (purchases.existsByBudgetCategoryId(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This category is linked to purchase groups. Keep its allocation and edit the amount instead.");
        }
        budgets.delete(category);
        return findByEvent(eventId);
    }
    public EventBudgetResponse findByEvent(Long eventId) {
        Event event = event(eventId);
        Map<String, BigDecimal> actuals = new LinkedHashMap<>();
        Map<String, String> names = new LinkedHashMap<>();
        for (Expense expense : expenses.findByEventIdOrderByExpenseDateDescIdDesc(eventId)) {
            String key = BudgetCategory.key(expense.getCategory());
            actuals.merge(key, expense.getAmount(), BigDecimal::add);
            names.putIfAbsent(key, expense.getCategory());
        }
        BigDecimal totalActual = actuals.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal allocated = BigDecimal.ZERO;
        List<EventBudgetResponse.Category> rows = new ArrayList<>();
        for (BudgetCategory budget : budgets.findByEventIdOrderByCategoryAscIdAsc(eventId)) {
            BigDecimal actual = actuals.getOrDefault(budget.getCategoryKey(), BigDecimal.ZERO);
            actuals.remove(budget.getCategoryKey());
            allocated = allocated.add(budget.getPlannedAmount());
            rows.add(new EventBudgetResponse.Category(budget.getId(), budget.getCategory(), budget.getOwner(),
                    budget.getPlannedAmount(), actual, budget.getPlannedAmount().subtract(actual)));
        }
        actuals.forEach((key, actual) -> rows.add(new EventBudgetResponse.Category(null, names.get(key), null,
                BigDecimal.ZERO, actual, actual.negate())));
        return new EventBudgetResponse(eventId, event.getPlannedBudget(), allocated,
                event.getPlannedBudget().subtract(allocated), totalActual, rows);
    }
}
