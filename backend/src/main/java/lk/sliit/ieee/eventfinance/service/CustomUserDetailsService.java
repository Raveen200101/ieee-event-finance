package lk.sliit.ieee.eventfinance.service;
import lk.sliit.ieee.eventfinance.config.ChapterPrincipal;
import lk.sliit.ieee.eventfinance.entity.ChapterUser;
import lk.sliit.ieee.eventfinance.repository.ChapterUserRepository;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
@Service
public class CustomUserDetailsService implements UserDetailsService {
    private final ChapterUserRepository users;
    public CustomUserDetailsService(ChapterUserRepository users) { this.users = users; }
    @Override public UserDetails loadUserByUsername(String email) {
        return new ChapterPrincipal(users.findByEmail(ChapterUser.normalizeEmail(email)).orElseThrow(() -> new UsernameNotFoundException("Invalid credentials")));
    }
}
