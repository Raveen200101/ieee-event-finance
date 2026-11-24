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
public class PurchaseService {
    private final EventRepository events;
    private final BudgetCategoryRepository budgets;
    private final PurchaseGroupRepository groups;
    private final QuotationRepository quotes;
    public PurchaseService(EventRepository events, BudgetCategoryRepository budgets,
            PurchaseGroupRepository groups, QuotationRepository quotes) {
        this.events = events; this.budgets = budgets; this.groups = groups; this.quotes = quotes;
    }
    private Event event(Long id) { return events.findById(id).orElseThrow(() -> new EventNotFoundException(id)); }
    private PurchaseGroup group(Long eventId, Long id) {
        event(eventId);
        return groups.findByIdAndEventId(id, eventId).orElseThrow(() -> missing("Purchase group"));
    }
    private Quotation quote(Long groupId, Long id) {
        return quotes.findByIdAndPurchaseGroupId(id, groupId).orElseThrow(() -> missing("Quotation"));
    }
    private ResponseStatusException missing(String name) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, name + " not found in this event or purchase group.");
    }
    public List<PurchaseGroupResponse> list(Long eventId) {
        event(eventId);
        var records = groups.findByEventIdOrderByIdAsc(eventId);
        var quotations = quotes.findByPurchaseGroupEventIdOrderByIdAsc(eventId);
        Map<Long, Long> categories = new HashMap<>();
        records.forEach(g -> categories.put(g.getId(), g.getBudgetCategory().getId()));
        Map<Long, BigDecimal> selectedTotals = new HashMap<>();
        quotations.stream().filter(Quotation::isSelected).forEach(q ->
                selectedTotals.merge(categories.get(q.getGroupId()), q.getTotal(), BigDecimal::add));
        return records.stream().map(g -> {
            var category = g.getBudgetCategory();
            var selected = selectedTotals.getOrDefault(category.getId(), BigDecimal.ZERO);
            var rows = quotations.stream().filter(q -> q.getGroupId().equals(g.getId())).map(q ->
                    new PurchaseGroupResponse.Quote(q.getId(), q.getSupplier(), q.getQuantity(), q.getUnitPrice(),
                            q.getTotal(), q.getNotes(), q.isSelected())).toList();
            return new PurchaseGroupResponse(g.getId(), g.getName(), g.getNotes(), category.getId(),
                    category.getCategory(), category.getPlannedAmount(), selected,
                    category.getPlannedAmount().subtract(selected), rows);
        }).toList();
    }
    @Transactional
    public List<PurchaseGroupResponse> create(Long eventId, CreatePurchaseGroupRequest request) {
        var event = event(eventId);
        var category = budgets.findByIdAndEventId(request.budgetCategoryId(), eventId)
                .orElseThrow(() -> missing("Budget category"));
        groups.saveAndFlush(new PurchaseGroup(event, category, request.name(), request.notes()));
        return list(eventId);
    }
    @Transactional
    public List<PurchaseGroupResponse> saveQuote(Long eventId, Long groupId, Long id, QuotationRequest request) {
        var group = group(eventId, groupId);
        if (id == null) quotes.saveAndFlush(new Quotation(group, request.supplier(), request.quantity(), request.unitPrice(), request.notes()));
        else quote(groupId, id).update(request.supplier(), request.quantity(), request.unitPrice(), request.notes());
        quotes.flush();
        return list(eventId);
    }
    @Transactional
    public List<PurchaseGroupResponse> select(Long eventId, Long groupId, Long id) {
        group(eventId, groupId); // Serializes quotation writes and selections within this group.
        if (id != null) quote(groupId, id);
        quotes.findByPurchaseGroupIdOrderByIdAsc(groupId).forEach(q -> q.setSelected(q.getId().equals(id)));
        quotes.flush();
        return list(eventId);
    }
    @Transactional
    public List<PurchaseGroupResponse> deleteQuote(Long eventId, Long groupId, Long id) {
        group(eventId, groupId);
        quotes.delete(quote(groupId, id));
        quotes.flush();
        return list(eventId);
    }
}
