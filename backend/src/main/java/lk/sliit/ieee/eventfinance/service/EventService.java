package lk.sliit.ieee.eventfinance.service;

import java.util.List;
import lk.sliit.ieee.eventfinance.dto.CreateEventRequest;
import lk.sliit.ieee.eventfinance.dto.EventResponse;
import lk.sliit.ieee.eventfinance.entity.Event;
import lk.sliit.ieee.eventfinance.exception.EventNotFoundException;
import lk.sliit.ieee.eventfinance.repository.EventRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class EventService {
    private final EventRepository repository;

    public EventService(EventRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public EventResponse create(CreateEventRequest request) {
        Event event = new Event(request.name().strip(), request.description().strip(),
                request.eventDate(), request.venue().strip(), request.plannedBudget());
        return toResponse(repository.save(event));
    }

    public List<EventResponse> findAll() {
        return repository.findAll(Sort.by(Sort.Order.desc("eventDate"), Sort.Order.desc("id")))
                .stream().map(this::toResponse).toList();
    }

    public EventResponse findById(Long id) {
        return toResponse(repository.findById(id).orElseThrow(() -> new EventNotFoundException(id)));
    }

    private EventResponse toResponse(Event event) {
        return new EventResponse(event.getId(), event.getName(), event.getDescription(),
                event.getEventDate(), event.getVenue(), event.getPlannedBudget());
    }
}
