package lk.sliit.ieee.eventfinance.config;

import org.springframework.context.annotation.*;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // Event management is public during the local development milestone.
        return http
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/api/events", "/api/events/**", "/actuator/health", "/actuator/health/**").permitAll()
                        .anyRequest().authenticated())
                .csrf(csrf -> csrf.ignoringRequestMatchers("/api/events", "/api/events/**"))
                .httpBasic(Customizer.withDefaults())
                .build();
    }
}
