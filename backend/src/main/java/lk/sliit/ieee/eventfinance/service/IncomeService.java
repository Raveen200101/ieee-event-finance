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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

@Service
@PreAuthorize("@access.finance(authentication,#eventId)")
@Transactional(readOnly = true)
public class IncomeService {
    private final AccessPolicy access;
    private final EventRepository events;
    private final IncomeRepository incomes;
    private final IncomeSourceService sources;
    private final ExpenseService expenses;

    public IncomeService(EventRepository events, IncomeRepository incomes, ExpenseService expenses,AccessPolicy access,IncomeSourceService sources) {
        this.access=access; this.sources=sources;
        this.events = events;
        this.incomes = incomes;
        this.expenses = expenses;
    }

    @Transactional
    public IncomeResponse create(Long eventId, CreateIncomeRequest request) {
        Event event = events.findById(eventId).orElseThrow(() -> new EventNotFoundException(eventId));
        sources.check(eventId,request.source());
        Income income = new Income(event, request.description().strip(),
                request.source().strip(), request.amount(), request.incomeDate());
        income.setCreatedBy(access.user());
        return toResponse(incomes.save(income));
    }

    public EventIncomesResponse findByEvent(Long eventId) {
        BigDecimal totalExpenses = expenses.findByEvent(eventId).totalSpent();
        List<IncomeResponse> records = incomes.findByEventIdOrderByIncomeDateDescIdDesc(eventId)
                .stream().filter(record -> access.owns(record.getCreatedById())).map(this::toResponse).toList();
        BigDecimal totalIncome = records.stream().map(IncomeResponse::amount)
                .reduce(new BigDecimal("0.00"), BigDecimal::add);
        return new EventIncomesResponse(eventId, totalIncome, totalExpenses,
                totalIncome.subtract(totalExpenses), records);
    }

    @Transactional
    public IncomeResponse update(Long eventId,Long id,CreateIncomeRequest request) {
        var record=incomes.findByIdAndEventId(id,eventId).orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND,"Record not found."));
        if(!access.owns(record.getCreatedById())) throw new org.springframework.security.access.AccessDeniedException("Access denied");
        sources.check(eventId,request.source());
        record.update(request.description(),request.source(),request.amount(),request.incomeDate());
        return toResponse(record);
    }
    private IncomeResponse toResponse(Income income) {
        return new IncomeResponse(income.getId(), income.getDescription(), income.getSource(),
                income.getAmount(), income.getIncomeDate());
    }
}
