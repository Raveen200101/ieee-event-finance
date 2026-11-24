package lk.sliit.ieee.eventfinance.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "events")
public class Event {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, length = 2000)
    private String description;

    @Column(nullable = false)
    private LocalDate eventDate;

    @Column(nullable = false, length = 255)
    private String venue;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal plannedBudget;

    @Column(length = 30)
    private String term;

    @Column(length = 150)
    private String financeLead;

    @Column(nullable = false, length = 20)
    private String status = "Planning";

    private Integer expectedParticipants;

    protected Event() {}

    public Event(String name, String description, LocalDate eventDate, String venue, BigDecimal plannedBudget) {
        this.name = name;
        this.description = description;
        this.eventDate = eventDate;
        this.venue = venue;
        this.plannedBudget = plannedBudget;
    }

    public void setSetupDetails(String term, String financeLead, String status, Integer expectedParticipants) {
        this.term = term;
        this.financeLead = financeLead;
        this.status = status;
        this.expectedParticipants = expectedParticipants;
    }

    public String getTerm() { return term; }
    public String getFinanceLead() { return financeLead; }
    public String getStatus() { return status; }
    public Integer getExpectedParticipants() { return expectedParticipants; }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public LocalDate getEventDate() { return eventDate; }
    public String getVenue() { return venue; }
    public BigDecimal getPlannedBudget() { return plannedBudget; }
}
