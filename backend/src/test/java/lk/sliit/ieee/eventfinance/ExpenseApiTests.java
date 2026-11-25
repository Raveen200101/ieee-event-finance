package lk.sliit.ieee.eventfinance;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.*;
import java.time.LocalDate;
import lk.sliit.ieee.eventfinance.entity.Event;
import lk.sliit.ieee.eventfinance.repository.EventRepository;
import lk.sliit.ieee.eventfinance.repository.ExpenseRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import static org.junit.jupiter.api.Assertions.*;

@org.springframework.test.context.ContextConfiguration(initializers = JwtTestInitializer.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ExpenseApiTests extends AuthenticatedApiTest {
    @LocalServerPort
    private int port;
    @Autowired
    private EventRepository events;
    @Autowired
    private ExpenseRepository expenses;

    private final JsonMapper mapper = JsonMapper.builder().build();

    @Test
    void tracksExpensesWithExactTotalsAndEventIsolation() throws Exception {
        Event first = events.save(new Event("Expense API test", "", LocalDate.of(2026, 12, 1),
                "SLIIT", new BigDecimal("100.00")));
        Event second = events.save(new Event("Other expense API test", "", LocalDate.of(2026, 12, 1),
                "SLIIT", new BigDecimal("50.00")));
        try {
            JsonNode empty = mapper.readTree(get(first.getId()).body());
            assertEquals(0, empty.get("expenses").size());
            assertEquals(0, empty.get("totalSpent").decimalValue().compareTo(BigDecimal.ZERO));
            HttpResponse<String> created = post(first.getId(), """
                    {"description":"  Lunch  ","category":"Food","amount":120.25,"expenseDate":"2026-12-01"}
                    """);
            assertEquals(201, created.statusCode(), created.body());
            JsonNode expense = mapper.readTree(created.body());
            assertEquals("Lunch", expense.get("description").asText());
            assertTrue(expenses.existsById(expense.get("id").asLong()));
            assertEquals(201, post(first.getId(), """
                    {"description":"Printing","category":"Printing","amount":0.10,"expenseDate":"2026-12-02"}
                    """).statusCode());
            JsonNode summary = mapper.readTree(get(first.getId()).body());
            assertEquals(2, summary.get("expenses").size());
            assertEquals(0, new BigDecimal("120.35").compareTo(summary.get("totalSpent").decimalValue()));
            assertEquals(0, new BigDecimal("-20.35").compareTo(summary.get("remainingBudget").decimalValue()));
            assertEquals("Printing", summary.get("expenses").get(0).get("description").asText());
            assertEquals(0, mapper.readTree(get(second.getId()).body()).get("expenses").size());
            for (String amount : new String[]{"0", "-1", "1.001", "10000000000000"}) {
                assertEquals(400, post(first.getId(),
                        "{\"description\":\"Invalid\",\"category\":\"Food\",\"amount\":" + amount
                                + ",\"expenseDate\":\"2026-12-01\"}").statusCode());
            }
            assertEquals(400, post(first.getId(), """
                    {"description":" ","category":"","amount":1,"expenseDate":"bad-date"}
                    """).statusCode());
            assertEquals(404, get(0L).statusCode());
            assertEquals(404, post(0L, """
                    {"description":"Missing","category":"Other","amount":1,"expenseDate":"2026-12-01"}
                    """).statusCode());
            assertEquals(2, expenses.findByEventIdOrderByExpenseDateDescIdDesc(first.getId()).size());
        } finally {
            expenses.deleteAll(expenses.findByEventIdOrderByExpenseDateDescIdDesc(first.getId()));
            events.deleteById(first.getId());
            events.deleteById(second.getId());
        }
    }

    private HttpResponse<String> get(Long eventId) throws Exception {
        return client.send(authenticatedRequest(uri(eventId)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> post(Long eventId, String body) throws Exception {
        return client.send(authenticatedRequest(uri(eventId))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private URI uri(Long eventId) {
        return URI.create("http://127.0.0.1:" + port + "/api/events/" + eventId + "/expenses");
    }
}
