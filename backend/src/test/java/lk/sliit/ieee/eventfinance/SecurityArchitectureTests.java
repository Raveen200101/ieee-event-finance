package lk.sliit.ieee.eventfinance;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.web.FilterChainProxy;
import static org.junit.jupiter.api.Assertions.*;
@org.springframework.test.context.ContextConfiguration(initializers=JwtTestInitializer.class)
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
class SecurityArchitectureTests {
    @Autowired FilterChainProxy security;
    @Test void usesBuiltInBearerAuthenticationAndStatelessCsrfWithoutCustomJwtFilters() {
        List<String> filters=security.getFilterChains().get(0).getFilters().stream().map(f -> f.getClass().getSimpleName()).toList();
        assertTrue(filters.contains("BearerTokenAuthenticationFilter"));assertTrue(filters.contains("CsrfFilter"));
        assertFalse(filters.contains("UsernamePasswordAuthenticationFilter"));assertFalse(filters.contains("BasicAuthenticationFilter"));
        assertTrue(filters.indexOf("CsrfFilter")<filters.indexOf("BearerTokenAuthenticationFilter"));
        assertTrue(filters.indexOf("BearerTokenAuthenticationFilter")<filters.indexOf("AuthorizationFilter"));
        System.out.println("VERIFIED_SECURITY_FILTER_ORDER="+String.join(" -> ",filters));
    }
}
