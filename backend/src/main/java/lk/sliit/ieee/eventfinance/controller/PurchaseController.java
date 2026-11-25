package lk.sliit.ieee.eventfinance.controller;
import jakarta.validation.Valid;
import java.util.List;
import lk.sliit.ieee.eventfinance.dto.*;
import lk.sliit.ieee.eventfinance.service.PurchaseService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/events/{eventId}/purchase-groups")
public class PurchaseController {
    private final PurchaseService service;
    public PurchaseController(PurchaseService service) { this.service = service; }
    @GetMapping public List<PurchaseGroupResponse> list(@PathVariable Long eventId) { return service.list(eventId); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public List<PurchaseGroupResponse> create(@PathVariable Long eventId, @Valid @RequestBody CreatePurchaseGroupRequest request) {
        return service.create(eventId, request);
    }
    @PostMapping("/{groupId}/quotations") @ResponseStatus(HttpStatus.CREATED)
    public List<PurchaseGroupResponse> add(@PathVariable Long eventId, @PathVariable Long groupId, @Valid @RequestBody QuotationRequest request) {
        return service.saveQuote(eventId, groupId, null, request);
    }
    @PutMapping("/{groupId}/quotations/{id}")
    public List<PurchaseGroupResponse> update(@PathVariable Long eventId, @PathVariable Long groupId, @PathVariable Long id, @Valid @RequestBody QuotationRequest request) {
        return service.saveQuote(eventId, groupId, id, request);
    }
    @DeleteMapping("/{groupId}/quotations/{id}")
    public List<PurchaseGroupResponse> delete(@PathVariable Long eventId, @PathVariable Long groupId, @PathVariable Long id) {
        return service.deleteQuote(eventId, groupId, id);
    }
    @PutMapping("/{groupId}/selection/{id}")
    public List<PurchaseGroupResponse> select(@PathVariable Long eventId, @PathVariable Long groupId, @PathVariable Long id) {
        return service.select(eventId, groupId, id);
    }
    @DeleteMapping("/{groupId}/selection")
    public List<PurchaseGroupResponse> clear(@PathVariable Long eventId, @PathVariable Long groupId) {
        return service.select(eventId, groupId, null);
    }
    @PutMapping("/{groupId}/owner") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void owner(@PathVariable Long eventId,@PathVariable Long groupId,@Valid @RequestBody AssignPurchaseOwnerRequest request) { service.assignOwner(eventId,groupId,request.userId()); }
    @DeleteMapping("/{groupId}/owner") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clearOwner(@PathVariable Long eventId,@PathVariable Long groupId) { service.clearOwner(eventId,groupId); }
}
