package lk.sliit.ieee.eventfinance.service;
import java.util.List;
import lk.sliit.ieee.eventfinance.entity.IncomeSource;
import lk.sliit.ieee.eventfinance.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
@Service @Transactional
public class IncomeSourceService {
    private final IncomeSourceRepository sources;private final EventRepository events;private final AccessPolicy access;
    public IncomeSourceService(IncomeSourceRepository sources,EventRepository events,AccessPolicy access) { this.sources=sources;this.events=events;this.access=access; }
    @PreAuthorize("@access.finance(authentication,#eventId)")
    public List<String> list(Long eventId) { return sources.findByEventIdOrderByNameAsc(eventId).stream().map(IncomeSource::getName).toList(); }
    @PreAuthorize("@access.committee(authentication)")
    public List<String> create(Long eventId,String name) { save(eventId,name);return sources.findByEventIdOrderByNameAsc(eventId).stream().map(IncomeSource::getName).toList(); }
    private void save(Long eventId,String name) {
        var event=events.findById(eventId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Event not found."));
        if(!sources.existsByEventIdAndNameKey(eventId,IncomeSource.key(name))) sources.saveAndFlush(new IncomeSource(event,name));
    }
    @PreAuthorize("@access.finance(authentication,#eventId)")
    public void check(Long eventId,String name) {
        if(access.committee()) save(eventId,name);
        else if(!sources.existsByEventIdAndNameKey(eventId,IncomeSource.key(name))) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Select an income category configured by the committee.");
    }
}
