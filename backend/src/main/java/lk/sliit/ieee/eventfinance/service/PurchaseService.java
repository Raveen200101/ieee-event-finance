package lk.sliit.ieee.eventfinance.service;

import java.math.BigDecimal;
import java.util.*;
import lk.sliit.ieee.eventfinance.dto.*;
import lk.sliit.ieee.eventfinance.entity.*;
import lk.sliit.ieee.eventfinance.exception.EventNotFoundException;
import lk.sliit.ieee.eventfinance.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class PurchaseService {
    private final AccessPolicy access;
    private final ChapterUserRepository users;
    private final EventMembershipRepository memberships;
    private final EventRepository events;
    private final BudgetCategoryRepository budgets;
    private final PurchaseGroupRepository groups;
    private final QuotationRepository quotes;
    public PurchaseService(EventRepository events, BudgetCategoryRepository budgets,
            PurchaseGroupRepository groups, QuotationRepository quotes,AccessPolicy access,ChapterUserRepository users,EventMembershipRepository memberships) {
        this.access=access;this.users=users;this.memberships=memberships;
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
    @PreAuthorize("@access.procurement(authentication,#eventId)")
    public List<PurchaseGroupResponse> list(Long eventId) {
        event(eventId);
        var records = groups.findByEventIdOrderByIdAsc(eventId).stream().filter(g -> access.committee() || access.user().getId().equals(g.getOwnerId())).toList();
        var quotations = quotes.findByPurchaseGroupEventIdOrderByIdAsc(eventId).stream().filter(q -> access.owns(q.getCreatedById())).toList();
        Map<Long, Long> categories = new HashMap<>();
        records.forEach(g -> categories.put(g.getId(), g.getBudgetCategory().getId()));
        Map<Long, BigDecimal> selectedTotals = new HashMap<>();
        quotations.stream().filter(q -> categories.containsKey(q.getGroupId())).filter(Quotation::isSelected).forEach(q ->
                selectedTotals.merge(categories.get(q.getGroupId()), q.getTotal(), BigDecimal::add));
        return records.stream().map(g -> {
            var category = g.getBudgetCategory();
            var selected = selectedTotals.getOrDefault(category.getId(), BigDecimal.ZERO);
            var rows = quotations.stream().filter(q -> q.getGroupId().equals(g.getId())).map(q ->
                    new PurchaseGroupResponse.Quote(q.getId(), q.getSupplier(), q.getQuantity(), q.getUnitPrice(),
                            q.getTotal(), q.getNotes(), q.isSelected())).toList();
            return new PurchaseGroupResponse(g.getId(), g.getName(), g.getNotes(), category.getId(),
                    category.getCategory(), access.committee()?category.getPlannedAmount():null, access.committee()?selected:null,
                    access.committee()?category.getPlannedAmount().subtract(selected):null, rows);
        }).toList();
    }
    @Transactional
    @PreAuthorize("@access.committee(authentication)")
    public List<PurchaseGroupResponse> create(Long eventId, CreatePurchaseGroupRequest request) {
        var event = event(eventId);
        var category = budgets.findByIdAndEventId(request.budgetCategoryId(), eventId)
                .orElseThrow(() -> missing("Budget category"));
        groups.saveAndFlush(new PurchaseGroup(event, category, request.name(), request.notes()));
        return list(eventId);
    }
    @Transactional
    @PreAuthorize("@access.purchase(authentication,#eventId,#groupId)")
    public List<PurchaseGroupResponse> saveQuote(Long eventId, Long groupId, Long id, QuotationRequest request) {
        var group = group(eventId, groupId);
        if (id == null) {
            var quotation=new Quotation(group,request.supplier(),request.quantity(),request.unitPrice(),request.notes()); quotation.setCreatedBy(access.user()); quotes.saveAndFlush(quotation);
        } else {
            var quotation=quote(groupId,id);
            if(!access.owns(quotation.getCreatedById())) throw new org.springframework.security.access.AccessDeniedException("Access denied");
            if(!access.committee() && quotation.isSelected()) throw new ResponseStatusException(HttpStatus.CONFLICT,"Only the committee can change a selected quotation.");
            quotation.update(request.supplier(),request.quantity(),request.unitPrice(),request.notes());
        }
        quotes.flush();
        return list(eventId);
    }
    @Transactional
    @PreAuthorize("@access.committee(authentication)")
    public List<PurchaseGroupResponse> select(Long eventId, Long groupId, Long id) {
        group(eventId, groupId); // Serializes quotation writes and selections within this group.
        if (id != null) quote(groupId, id);
        quotes.findByPurchaseGroupIdOrderByIdAsc(groupId).forEach(q -> q.setSelected(q.getId().equals(id)));
        quotes.flush();
        return list(eventId);
    }
    @Transactional
    @PreAuthorize("@access.purchase(authentication,#eventId,#groupId)")
    public List<PurchaseGroupResponse> deleteQuote(Long eventId, Long groupId, Long id) {
        group(eventId, groupId);
        var quotation=quote(groupId,id);
        if(!access.owns(quotation.getCreatedById())) throw new org.springframework.security.access.AccessDeniedException("Access denied");
        if(!access.committee() && quotation.isSelected()) throw new ResponseStatusException(HttpStatus.CONFLICT,"Only the committee can remove a selected quotation.");
        quotes.delete(quotation);
        quotes.flush();
        return list(eventId);
    }
    @Transactional @PreAuthorize("@access.committee(authentication)")
    public void assignOwner(Long eventId,Long groupId,Long userId) {
        var group=group(eventId,groupId);
        var owner=users.findById(userId).filter(u -> u.isEnabled() && u.getRole()==ChapterRole.LOGISTICS_TEAM_LEAD).orElseThrow(() -> missing("Logistics team lead"));
        if(!memberships.existsByEventIdAndUserId(eventId,userId)) throw new org.springframework.web.server.ResponseStatusException(HttpStatus.BAD_REQUEST,"Assign this lead to the event first.");
        group.assignOwner(owner);
    }
}
