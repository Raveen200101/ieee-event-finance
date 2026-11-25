package lk.sliit.ieee.eventfinance;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.*;
import java.time.LocalDate;
import lk.sliit.ieee.eventfinance.entity.Event;
import lk.sliit.ieee.eventfinance.entity.Expense;
import lk.sliit.ieee.eventfinance.repository.ExpenseRepository;
import lk.sliit.ieee.eventfinance.repository.EventRepository;
import lk.sliit.ieee.eventfinance.repository.IncomeRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import static org.junit.jupiter.api.Assertions.*;

@org.springframework.test.context.ContextConfiguration(initializers = JwtTestInitializer.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class IncomeApiTests extends AuthenticatedApiTest {
    @LocalServerPort
    private int port;
    @Autowired
    private lk.sliit.ieee.eventfinance.repository.IncomeSourceRepository sources;
    @Autowired
    private EventRepository events;
    @Autowired
    private IncomeRepository incomes;
    @Autowired
    private ExpenseRepository expenses;

    private final JsonMapper mapper = JsonMapper.builder().build();

    @Test
    void tracksIncomesWithExactTotalsAndEventIsolation() throws Exception {
        Event first = events.save(new Event("Income API test", "", LocalDate.of(2026, 12, 1),
                "SLIIT", new BigDecimal("100.00")));
        Event second = events.save(new Event("Other income API test", "", LocalDate.of(2026, 12, 1),
                "SLIIT", new BigDecimal("50.00")));
        expenses.save(new Expense(first, "Test cost", "Other", new BigDecimal("150.00"),
                LocalDate.of(2026, 12, 1)));
        try {
            JsonNode empty = mapper.readTree(get(first.getId()).body());
            assertEquals(0, empty.get("incomes").size());
            assertEquals(0, empty.get("totalIncome").decimalValue().compareTo(BigDecimal.ZERO));
            assertEquals(0, new BigDecimal("-150.00").compareTo(empty.get("balance").decimalValue()));
            HttpResponse<String> created = post(first.getId(), """
                    {"description":"  Lunch  ","source":"Food","amount":120.25,"incomeDate":"2026-12-01"}
                    """);
            assertEquals(201, created.statusCode(), created.body());
            JsonNode income = mapper.readTree(created.body());
            assertEquals("Lunch", income.get("description").asText());
            assertTrue(incomes.existsById(income.get("id").asLong()));
            assertEquals(201, post(first.getId(), """
                    {"description":"Printing","source":"Printing","amount":0.10,"incomeDate":"2026-12-02"}
                    """).statusCode());
            JsonNode summary = mapper.readTree(get(first.getId()).body());
            assertEquals(2, summary.get("incomes").size());
            assertEquals(0, new BigDecimal("150.00").compareTo(summary.get("totalExpenses").decimalValue()));
            assertEquals(0, new BigDecimal("120.35").compareTo(summary.get("totalIncome").decimalValue()));
            assertEquals(0, new BigDecimal("-29.65").compareTo(summary.get("balance").decimalValue()));
            assertEquals("Printing", summary.get("incomes").get(0).get("description").asText());
            assertEquals(0, mapper.readTree(get(second.getId()).body()).get("incomes").size());
            for (String amount : new String[]{"0", "-1", "1.001", "10000000000000"}) {
                assertEquals(400, post(first.getId(),
                        "{\"description\":\"Invalid\",\"source\":\"Food\",\"amount\":" + amount
                                + ",\"incomeDate\":\"2026-12-01\"}").statusCode());
            }
            assertEquals(400, post(first.getId(), """
                    {"description":" ","source":"","amount":1,"incomeDate":"bad-date"}
                    """).statusCode());
            assertEquals(404, get(0L).statusCode());
            assertEquals(404, post(0L, """
                    {"description":"Missing","source":"Other","amount":1,"incomeDate":"2026-12-01"}
                    """).statusCode());
            assertEquals(2, incomes.findByEventIdOrderByIncomeDateDescIdDesc(first.getId()).size());
        } finally {
            incomes.deleteAll(incomes.findByEventIdOrderByIncomeDateDescIdDesc(first.getId()));
            expenses.deleteAll(expenses.findByEventIdOrderByExpenseDateDescIdDesc(first.getId()));
            sources.deleteAll(sources.findByEventIdOrderByNameAsc(first.getId()));
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
        return URI.create("http://127.0.0.1:" + port + "/api/events/" + eventId + "/incomes");
    }
}
