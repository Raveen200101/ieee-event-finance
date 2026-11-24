package lk.sliit.ieee.eventfinance.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import lk.sliit.ieee.eventfinance.dto.*;
import lk.sliit.ieee.eventfinance.service.IncomeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/events/{eventId}/incomes")
public class IncomeController {
    private final IncomeService service;

    public IncomeController(IncomeService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<IncomeResponse> create(@PathVariable Long eventId,
                                                 @Valid @RequestBody CreateIncomeRequest request) {
        IncomeResponse income = service.create(eventId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(income);
    }

    @GetMapping
    public EventIncomesResponse findByEvent(@PathVariable Long eventId) {
        return service.findByEvent(eventId);
    }
}
