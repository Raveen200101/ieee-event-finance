package lk.sliit.ieee.eventfinance;

import java.net.URI;
import java.net.http.*;
import lk.sliit.ieee.eventfinance.repository.EventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class EventApiTests {
    @LocalServerPort
    private int port;

    @Autowired
    private EventRepository repository;

    private final HttpClient client = HttpClient.newHttpClient();
    private final JsonMapper mapper = JsonMapper.builder().build();

    @Test
    void createsPersistsListsAndRetrievesAnEvent() throws Exception {
        HttpResponse<String> created = post("""
                {"name":"  Integration test event  ","description":"Test description",
                 "eventDate":"2026-12-01","venue":"SLIIT","plannedBudget":50000.25}
                """);
        assertEquals(201, created.statusCode(), created.body());
        JsonNode event = mapper.readTree(created.body());
        long id = event.get("id").asLong();
        try {
            assertEquals("Integration test event", event.get("name").asText());
            assertTrue(repository.existsById(id));
            HttpResponse<String> details = get("/api/events/" + id);
            assertEquals(200, details.statusCode());
            assertEquals(id, mapper.readTree(details.body()).get("id").asLong());
            assertEquals("/api/events/" + id, created.headers().firstValue("Location").orElseThrow());
            HttpResponse<String> list = get("/api/events");
            assertEquals(200, list.statusCode());
            assertTrue(mapper.readTree(list.body()).isArray());
            assertTrue(list.body().contains("Integration test event"));
        } finally {
            repository.deleteById(id);
        }
    }

    @Test
    void rejectsInvalidEventsAndReportsMissingRecords() throws Exception {
        long before = repository.count();
        HttpResponse<String> response = post("""
                {"name":" ","description":"","eventDate":"2026-12-01","venue":"",
                 "plannedBudget":-1}
                """);
        assertEquals(400, response.statusCode(), response.body());
        JsonNode errors = mapper.readTree(response.body()).get("errors");
        assertTrue(errors.has("name"));
        assertTrue(errors.has("venue"));
        assertTrue(errors.has("plannedBudget"));
        assertEquals(before, repository.count());
        assertEquals(400, post("{invalid").statusCode());
        assertEquals(400, get("/api/events/invalid").statusCode());
        assertEquals(404, get("/api/events/0").statusCode());
    }

    private HttpResponse<String> post(String body) throws Exception {
        return client.send(HttpRequest.newBuilder(uri("/api/events"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> get(String path) throws Exception {
        return client.send(HttpRequest.newBuilder(uri(path)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private URI uri(String path) {
        return URI.create("http://127.0.0.1:" + port + path);
    }
}
