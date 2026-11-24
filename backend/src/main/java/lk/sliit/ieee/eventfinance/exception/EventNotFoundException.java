package lk.sliit.ieee.eventfinance.exception;

public class EventNotFoundException extends RuntimeException {
    public EventNotFoundException(Long id) {
        super("Event " + id + " was not found.");
    }
}
