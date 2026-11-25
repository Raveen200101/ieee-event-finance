package lk.sliit.ieee.eventfinance;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.*;
import java.time.LocalDate;
import java.util.concurrent.CompletableFuture;
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
class PurchaseApiTests extends AuthenticatedApiTest {
    @LocalServerPort private int port;
    @Autowired private EventRepository events;
    @Autowired private BudgetCategoryRepository budgets;
    @Autowired private PurchaseGroupRepository groups;
    @Autowired private QuotationRepository quotations;
    @Autowired private ExpenseRepository expenses;
    private final JsonMapper mapper = JsonMapper.builder().build();

    @Test
    void comparesSelectsAndIsolatesQuotationsWithoutCreatingExpenses() throws Exception {
        Event event = events.save(new Event("Purchase API test", "", LocalDate.of(2026, 12, 1), "SLIIT", new BigDecimal("100.00")));
        Event other = events.save(new Event("Other purchase API test", "", LocalDate.of(2026, 12, 1), "SLIIT", new BigDecimal("100.00")));
        BudgetCategory category = budgets.save(new BudgetCategory(event, "Merchandise", "Lead", new BigDecimal("1.00")));
        try {
            String base = base(event.getId());
            assertEquals(0, json(call(base, "GET", null)).size());
            var created = call(base, "POST", groupBody(category.getId(), " T-shirts "));
            assertEquals(201, created.statusCode(), created.body());
            long groupId = json(created).get(0).get("id").asLong();
            assertEquals("T-shirts", json(created).get(0).get("name").asText());
            String group = base + "/" + groupId;
            var first = call(group + "/quotations", "POST", quoteBody(" Supplier A ", 3, "0.10"));
            assertEquals(201, first.statusCode(), first.body());
            long firstId = json(first).get(0).get("quotations").get(0).get("id").asLong();
            amount("0.30", json(first).get(0).get("quotations").get(0), "total");
            assertEquals("Supplier A", quotations.findById(firstId).orElseThrow().getSupplier());
            var second = call(group + "/quotations", "POST", quoteBody("Supplier B", 2, "0.20"));
            long secondId = json(second).get(0).get("quotations").get(1).get("id").asLong();
            assertEquals(200, call(group + "/selection/" + firstId, "PUT", null).statusCode());
            var selected = call(group + "/selection/" + secondId, "PUT", null);
            assertEquals(200, selected.statusCode(), selected.body());
            var records = quotations.findByPurchaseGroupIdOrderByIdAsc(groupId);
            assertFalse(records.get(0).isSelected());
            assertTrue(records.get(1).isSelected());
            amount("0.40", json(selected).get(0), "categorySelectedTotal");
            assertEquals(201, call(base, "POST", groupBody(category.getId(), "Badges")).statusCode());
            long anotherGroup = json(call(base, "GET", null)).get(1).get("id").asLong();
            var another = call(base + "/" + anotherGroup + "/quotations", "POST", quoteBody("Supplier C", 2, "0.40"));
            long anotherId = json(another).get(1).get("quotations").get(0).get("id").asLong();
            var combined = call(base + "/" + anotherGroup + "/selection/" + anotherId, "PUT", null);
            amount("1.20", json(combined).get(0), "categorySelectedTotal");
            amount("-0.20", json(combined).get(1), "categoryRemaining");
            assertEquals(0, expenses.findByEventIdOrderByExpenseDateDescIdDesc(event.getId()).size());
            assertEquals(409, call("/api/events/" + event.getId() + "/budget/" + category.getId(), "DELETE", null).statusCode());
            // IDs must belong to the addressed event and group.
            assertEquals(404, call(base(other.getId()) + "/" + groupId + "/quotations", "POST", quoteBody("Invalid", 1, "1")).statusCode());
            assertEquals(404, call(group + "/selection/" + anotherId, "PUT", null).statusCode());
            assertEquals(404, call(group + "/quotations/" + anotherId, "PUT", quoteBody("Invalid", 1, "1")).statusCode());
            assertEquals(404, call(group + "/quotations/" + anotherId, "DELETE", null).statusCode());
            assertEquals(404, call(base(other.getId()), "POST", groupBody(category.getId(), "Invalid")).statusCode());
            assertEquals(404, call(base(0L), "GET", null).statusCode());
            assertEquals(0, json(call(base(other.getId()), "GET", null)).size());
            for (String price : new String[]{"0", "-1", "0.001", "10000000000000", "null"}) {
                assertEquals(400, call(group + "/quotations", "POST", quoteBody("Invalid", 1, price)).statusCode());
            }
            for (int quantity : new int[]{0, -1, 1000001}) {
                assertEquals(400, call(group + "/quotations", "POST", quoteBody("Invalid", quantity, "1")).statusCode());
            }
            assertEquals(400, call(group + "/quotations", "POST", quoteBody(" ", 1, "1")).statusCode());
            assertEquals(400, call(base, "POST", groupBody(category.getId(), " ")).statusCode());
            // Two simultaneous preferences still leave exactly one selected quotation.
            var a = CompletableFuture.supplyAsync(() -> select(group, firstId));
            var b = CompletableFuture.supplyAsync(() -> select(group, secondId));
            assertEquals(200, a.get()); assertEquals(200, b.get());
            assertEquals(1, quotations.findByPurchaseGroupIdOrderByIdAsc(groupId).stream().filter(Quotation::isSelected).count());
            assertEquals(200, call(group + "/selection/" + secondId, "PUT", null).statusCode());
            assertEquals(200, call(group + "/quotations/" + secondId, "PUT", quoteBody("Supplier B revised", 4, "0.25")).statusCode());
            assertFalse(quotations.findById(secondId).orElseThrow().isSelected());
            amount("1.00", json(call(base, "GET", null)).get(0).get("quotations").get(1), "total");
            assertEquals(200, call(group + "/selection/" + secondId, "PUT", null).statusCode());
            assertEquals(200, call(group + "/selection", "DELETE", null).statusCode());
            assertTrue(quotations.findByPurchaseGroupIdOrderByIdAsc(groupId).stream().noneMatch(Quotation::isSelected));
            assertEquals(200, call(group + "/selection/" + secondId, "PUT", null).statusCode());
            assertEquals(200, call(group + "/quotations/" + secondId, "DELETE", null).statusCode());
            assertFalse(quotations.existsById(secondId));
            assertEquals(404, call(group + "/quotations/" + secondId, "DELETE", null).statusCode());
            assertEquals(0, expenses.findByEventIdOrderByExpenseDateDescIdDesc(event.getId()).size());
        } finally {
            quotations.deleteAll(quotations.findByPurchaseGroupEventIdOrderByIdAsc(event.getId()));
            groups.deleteAll(groups.findByEventIdOrderByIdAsc(event.getId()));
            budgets.deleteById(category.getId());
            events.deleteById(event.getId()); events.deleteById(other.getId());
        }
    }
    private int select(String group, long id) {
        try { return call(group + "/selection/" + id, "PUT", null).statusCode(); }
        catch (Exception exception) { throw new RuntimeException(exception); }
    }
    private String base(Long eventId) { return "/api/events/" + eventId + "/purchase-groups"; }
    private String groupBody(Long categoryId, String name) {
        return "{\"name\":\"" + name + "\",\"budgetCategoryId\":" + categoryId + ",\"notes\":\"Compare suppliers\"}";
    }
    private String quoteBody(String supplier, int quantity, String price) {
        return "{\"supplier\":\"" + supplier + "\",\"quantity\":" + quantity + ",\"unitPrice\":" + price + ",\"notes\":\"Includes delivery\"}";
    }
    private void amount(String expected, JsonNode node, String field) {
        assertEquals(0, new BigDecimal(expected).compareTo(node.get(field).decimalValue()), field);
    }
    private JsonNode json(HttpResponse<String> response) throws Exception { return mapper.readTree(response.body()); }
    private HttpResponse<String> call(String path, String method, String body) throws Exception {
        return client.send(authenticatedRequest(URI.create("http://127.0.0.1:" + port + path))
                .header("Content-Type", "application/json")
                .method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
    }
}
