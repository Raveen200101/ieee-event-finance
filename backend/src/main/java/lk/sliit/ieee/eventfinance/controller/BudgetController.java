package lk.sliit.ieee.eventfinance.controller;

import jakarta.validation.Valid;
import lk.sliit.ieee.eventfinance.dto.*;
import lk.sliit.ieee.eventfinance.service.BudgetService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/events/{eventId}/budget")
public class BudgetController {
    private final BudgetService service;
    public BudgetController(BudgetService service) { this.service = service; }
    @GetMapping
    public EventBudgetResponse get(@PathVariable Long eventId) { return service.findByEvent(eventId); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public EventBudgetResponse create(@PathVariable Long eventId, @Valid @RequestBody CreateBudgetCategoryRequest request) {
        return service.create(eventId, request);
    }
    @PutMapping("/{id}")
    public EventBudgetResponse update(@PathVariable Long eventId, @PathVariable Long id,
            @Valid @RequestBody UpdateBudgetCategoryRequest request) { return service.update(eventId, id, request); }
    @DeleteMapping("/{id}")
    public EventBudgetResponse delete(@PathVariable Long eventId, @PathVariable Long id) { return service.delete(eventId, id); }
}
