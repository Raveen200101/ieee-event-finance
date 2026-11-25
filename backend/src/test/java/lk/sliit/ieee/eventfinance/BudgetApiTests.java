package lk.sliit.ieee.eventfinance;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.*;
import java.time.LocalDate;
import lk.sliit.ieee.eventfinance.entity.*;
import lk.sliit.ieee.eventfinance.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import static org.junit.jupiter.api.Assertions.*;

@org.springframework.test.context.ContextConfiguration(initializers = JwtTestInitializer.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BudgetApiTests extends AuthenticatedApiTest {
    @LocalServerPort private int port;
    @Autowired private EventRepository events;
    @Autowired private ExpenseRepository expenses;
    @Autowired private BudgetCategoryRepository budgets;
    private final JsonMapper mapper = JsonMapper.builder().build();

    @Test
    void managesAllocationsAndPreservesActualExpenses() throws Exception {
        Event first = events.save(new Event("Budget API test", "", LocalDate.of(2026, 12, 1), "SLIIT", new BigDecimal("100.00")));
        Event second = events.save(new Event("Other budget API test", "", LocalDate.of(2026, 12, 1), "SLIIT", new BigDecimal("50.00")));
        try {
            assertEquals(0, json(call(first.getId(), "", "GET", null)).get("categories").size());
            expenses.save(new Expense(first, "Lunch", " FOOD ", new BigDecimal("60.25"), LocalDate.of(2026, 12, 1)));
            expenses.save(new Expense(first, "Tea", "food", new BigDecimal("0.10"), LocalDate.of(2026, 12, 1)));
            expenses.save(new Expense(first, "Bus", "Transport", new BigDecimal("10.00"), LocalDate.of(2026, 12, 1)));
            HttpResponse<String> created = call(first.getId(), "", "POST", """
                    {"category":" Food ","owner":" Finance lead ","plannedAmount":50.00}
                    """);
            assertEquals(201, created.statusCode(), created.body());
            JsonNode summary = json(created);
            assertEquals(2, summary.get("categories").size());
            JsonNode food = summary.get("categories").get(0);
            long id = food.get("id").asLong();
            assertEquals("Food", food.get("category").asText());
            assertEquals("Finance lead", food.get("owner").asText());
            amount("60.35", food, "actualAmount");
            amount("-10.35", food, "remainingAmount");
            amount("70.35", summary, "totalActual");
            assertTrue(summary.get("categories").get(1).get("id").isNull());
            assertEquals(409, call(first.getId(), "", "POST", """
                    {"category":"fOoD","owner":"Other","plannedAmount":1}
                    """).statusCode());
            assertEquals(0, json(call(second.getId(), "", "GET", null)).get("categories").size());
            assertEquals(404, call(second.getId(), "/" + id, "PUT", """
                    {"owner":"Wrong event","plannedAmount":1}
                    """).statusCode());
            assertEquals(404, call(second.getId(), "/" + id, "DELETE", null).statusCode());
            HttpResponse<String> updated = call(first.getId(), "/" + id, "PUT", """
                    {"owner":"New lead","plannedAmount":120.10}
                    """);
            assertEquals(200, updated.statusCode(), updated.body());
            amount("-20.10", json(updated), "unallocatedBudget");
            assertEquals("New lead", json(updated).get("categories").get(0).get("owner").asText());
            assertEquals(0, new BigDecimal("120.10").compareTo(budgets.findById(id).orElseThrow().getPlannedAmount()));
            for (String value : new String[]{"-1", "1.001", "10000000000000", "null"}) {
                assertEquals(400, call(first.getId(), "", "POST", "{\"category\":\"Invalid\",\"owner\":\"Lead\",\"plannedAmount\":" + value + "}").statusCode());
            }
            assertEquals(400, call(first.getId(), "", "POST", """
                    {"category":" ","owner":" ","plannedAmount":1}
                    """).statusCode());
            assertEquals(400, call(first.getId(), "/" + id, "PUT", """
                    {"owner":" ","plannedAmount":-1}
                    """).statusCode());
            assertEquals(404, call(0L, "", "GET", null).statusCode());
            assertEquals(404, call(0L, "", "POST", """
                    {"category":"Food","owner":"Lead","plannedAmount":0}
                    """).statusCode());
            assertEquals(404, call(first.getId(), "/0", "PUT", """
                    {"owner":"Lead","plannedAmount":0}
                    """).statusCode());
            HttpResponse<String> deleted = call(first.getId(), "/" + id, "DELETE", null);
            assertEquals(200, deleted.statusCode(), deleted.body());
            amount("0", json(deleted), "totalAllocated");
            amount("70.35", json(deleted), "totalActual");
            assertEquals(3, expenses.findByEventIdOrderByExpenseDateDescIdDesc(first.getId()).size());
            assertEquals(404, call(first.getId(), "/" + id, "DELETE", null).statusCode());
            assertEquals(201, call(first.getId(), "", "POST", """
                    {"category":"Food","owner":"Lead","plannedAmount":0}
                    """).statusCode());
        } finally {
            budgets.deleteAll(budgets.findByEventIdOrderByCategoryAscIdAsc(first.getId()));
            expenses.deleteAll(expenses.findByEventIdOrderByExpenseDateDescIdDesc(first.getId()));
            events.deleteById(first.getId());
            events.deleteById(second.getId());
        }
    }
    private void amount(String expected, JsonNode node, String field) {
        assertEquals(0, new BigDecimal(expected).compareTo(node.get(field).decimalValue()), field);
    }
    private JsonNode json(HttpResponse<String> response) throws Exception { return mapper.readTree(response.body()); }
    private HttpResponse<String> call(Long eventId, String suffix, String method, String body) throws Exception {
        return client.send(authenticatedRequest(URI.create("http://127.0.0.1:" + port + "/api/events/" + eventId + "/budget" + suffix))
                .header("Content-Type", "application/json")
                .method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body)).build(),
                HttpResponse.BodyHandlers.ofString());
    }
}
