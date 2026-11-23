package lk.sliit.ieee.eventfinance.controller;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import lk.sliit.ieee.eventfinance.dto.*;
import lk.sliit.ieee.eventfinance.service.EventService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/events")
public class EventController {
    private final EventService service;

    public EventController(EventService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<EventResponse> create(@Valid @RequestBody CreateEventRequest request) {
        EventResponse event = service.create(request);
        return ResponseEntity.created(URI.create("/api/events/" + event.id())).body(event);
    }

    @GetMapping
    public List<EventResponse> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public EventResponse findById(@PathVariable Long id) {
        return service.findById(id);
    }
}
