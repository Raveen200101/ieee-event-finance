package lk.sliit.ieee.eventfinance.dto;
import java.time.LocalDate;
/** Event metadata available to volunteers; financial amounts are deliberately omitted. */
public record PublicEventResponse(Long id,String name,String description,LocalDate eventDate,String venue,String term,String status,Integer expectedParticipants) {
    public static PublicEventResponse from(EventResponse event) {
        return new PublicEventResponse(event.id(),event.name(),event.description(),event.eventDate(),event.venue(),event.term(),event.status(),event.expectedParticipants());
    }
}
