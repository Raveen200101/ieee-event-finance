package lk.sliit.ieee.eventfinance.controller;
import java.util.List;
import lk.sliit.ieee.eventfinance.dto.EventMemberResponse;
import lk.sliit.ieee.eventfinance.service.EventMembershipService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/events/{eventId}/members")
public class EventMembershipController {
    private final EventMembershipService service;
    public EventMembershipController(EventMembershipService service) { this.service=service; }
    @GetMapping public List<Long> list(@PathVariable Long eventId) { return service.list(eventId); }
    @GetMapping("/options") public List<EventMemberResponse> options(@PathVariable Long eventId) { return service.options(eventId); }
    @PutMapping("/{userId}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void assign(@PathVariable Long eventId,@PathVariable Long userId) { service.assign(eventId,userId); }
    @DeleteMapping("/{userId}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@PathVariable Long eventId,@PathVariable Long userId) { service.remove(eventId,userId); }
}
