package lk.sliit.ieee.eventfinance.config;
import java.io.IOException;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
@Component
public class SecurityErrors {
    public void write(HttpServletResponse response, int status) throws IOException {
        response.setStatus(status); response.setContentType("application/problem+json");
        if (status == 401) response.setHeader("WWW-Authenticate", "Bearer");
        response.getWriter().write("{\"status\":" + status + ",\"detail\":\"" + (status == 401 ? "Authentication required or token invalid." : "Access denied or CSRF token invalid.") + "\"}");
    }
}
