package lk.sliit.ieee.eventfinance.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import lk.sliit.ieee.eventfinance.dto.*;
import lk.sliit.ieee.eventfinance.service.ExpenseService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/events/{eventId}/expenses")
public class ExpenseController {
    private final ExpenseService service;

    public ExpenseController(ExpenseService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ExpenseResponse> create(@PathVariable Long eventId,
                                                 @Valid @RequestBody CreateExpenseRequest request) {
        ExpenseResponse expense = service.create(eventId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(expense);
    }

    @GetMapping
    public EventExpensesResponse findByEvent(@PathVariable Long eventId) {
        return service.findByEvent(eventId);
    }
    @PutMapping("/{id}")
    public ExpenseResponse update(@PathVariable Long eventId,@PathVariable Long id,@Valid @RequestBody CreateExpenseRequest request) { return service.update(eventId,id,request); }
}
