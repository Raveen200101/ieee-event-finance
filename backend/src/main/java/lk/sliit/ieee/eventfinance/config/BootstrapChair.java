package lk.sliit.ieee.eventfinance.config;
import lk.sliit.ieee.eventfinance.dto.CreateUserRequest;
import lk.sliit.ieee.eventfinance.entity.ChapterRole;
import lk.sliit.ieee.eventfinance.repository.ChapterUserRepository;
import lk.sliit.ieee.eventfinance.service.ChapterUserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
@Component
public class BootstrapChair implements ApplicationRunner {
    private final ChapterUserRepository users;
    private final ChapterUserService service;
    private final String email;
    private final String password;
    public BootstrapChair(ChapterUserRepository users, ChapterUserService service,
            @Value("${APP_BOOTSTRAP_CHAIR_EMAIL:}") String email, @Value("${APP_BOOTSTRAP_CHAIR_PASSWORD:}") String password) {
        this.users = users; this.service = service; this.email = email; this.password = password;
    }
    @Override public void run(ApplicationArguments args) {
        if (users.count() != 0 || email.isBlank()) return;
        if (email.strip().length() > 254 || !email.strip().matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) {
            throw new IllegalStateException("APP_BOOTSTRAP_CHAIR_EMAIL must be a valid email address.");
        }
        service.bootstrap(new CreateUserRequest(email, "Chapter Chair", password, ChapterRole.COMMITTEE_MEMBER));
    }
}
