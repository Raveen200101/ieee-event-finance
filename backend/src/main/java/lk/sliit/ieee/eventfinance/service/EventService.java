package lk.sliit.ieee.eventfinance.service;

import java.util.List;
import lk.sliit.ieee.eventfinance.dto.CreateEventRequest;
import lk.sliit.ieee.eventfinance.dto.EventResponse;
import lk.sliit.ieee.eventfinance.entity.Event;
import lk.sliit.ieee.eventfinance.exception.EventNotFoundException;
import lk.sliit.ieee.eventfinance.repository.EventRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

@Service
@PreAuthorize("hasAnyRole('COMMITTEE_MEMBER','VOLUNTEER','FINANCE_TEAM_LEAD','LOGISTICS_TEAM_LEAD')")
@Transactional(readOnly = true)
public class EventService {
    private final EventRepository repository;
    private final AccessPolicy access;

    public EventService(EventRepository repository,AccessPolicy access) {
        this.access=access;
        this.repository = repository;
    }

    @Transactional
    @PreAuthorize("@access.committee(authentication)")
    public EventResponse create(CreateEventRequest request) {
        Event event = new Event(request.name().strip(), request.description().strip(),
                request.eventDate(), request.venue().strip(), request.plannedBudget());
        event.setSetupDetails(request.term() == null ? null : request.term().strip(),
                request.financeLead() == null ? null : request.financeLead().strip(),
                request.status() == null ? "Planning" : request.status(), request.expectedParticipants());
        return toResponse(repository.save(event));
    }

    public List<EventResponse> findAll() {
        return (access.committee() ? repository.findAll(Sort.by(Sort.Order.desc("eventDate"), Sort.Order.desc("id"))) : repository.findAssigned(access.user().getId()))
                .stream().map(this::toResponse).toList();
    }

    @PreAuthorize("@access.event(authentication,#id)")
    public EventResponse findById(Long id) {
        return toResponse(repository.findById(id).orElseThrow(() -> new EventNotFoundException(id)));
    }

    private EventResponse toResponse(Event event) {
        return new EventResponse(event.getId(), event.getName(), event.getDescription(),
                event.getEventDate(), event.getVenue(), event.getPlannedBudget(), event.getTerm(),
                event.getFinanceLead(), event.getStatus(), event.getExpectedParticipants());
    }
}
