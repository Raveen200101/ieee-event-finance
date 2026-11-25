package lk.sliit.ieee.eventfinance.controller;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import lk.sliit.ieee.eventfinance.dto.*;
import lk.sliit.ieee.eventfinance.service.EventService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;

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
    public List<?> findAll(Authentication authentication) {
        var events = service.findAll();
        return volunteer(authentication) ? events.stream().map(PublicEventResponse::from).toList() : events;
    }

    @GetMapping("/{id}")
    public Object findById(@PathVariable Long id, Authentication authentication) {
        var event = service.findById(id);
        return volunteer(authentication) ? PublicEventResponse.from(event) : event;
    }
    private boolean volunteer(Authentication authentication) {
        return authentication.getAuthorities().stream().noneMatch(a -> a.getAuthority().equals("ROLE_COMMITTEE_MEMBER"));
    }
}
