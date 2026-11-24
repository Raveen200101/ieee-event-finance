package lk.sliit.ieee.eventfinance.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record EventResponse(Long id, String name, String description, LocalDate eventDate,
                            String venue, BigDecimal plannedBudget, String term, String financeLead,
                            String status, Integer expectedParticipants) {}
