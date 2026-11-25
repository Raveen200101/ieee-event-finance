package lk.sliit.ieee.eventfinance.controller;
import java.util.List;
import jakarta.validation.Valid;
import lk.sliit.ieee.eventfinance.dto.IncomeSourceRequest;
import lk.sliit.ieee.eventfinance.service.IncomeSourceService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/events/{eventId}/income-sources")
public class IncomeSourceController {
    private final IncomeSourceService service;
    public IncomeSourceController(IncomeSourceService service) { this.service=service; }
    @GetMapping public List<String> list(@PathVariable Long eventId) { return service.list(eventId); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public List<String> create(@PathVariable Long eventId,@Valid @RequestBody IncomeSourceRequest request) { return service.create(eventId,request.name()); }
}
