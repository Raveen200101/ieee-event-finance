package lk.sliit.ieee.eventfinance.config;
import java.util.List;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.*;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.*;
import org.springframework.web.cors.*;
import lk.sliit.ieee.eventfinance.service.CustomUserDetailsService;
@Configuration @EnableMethodSecurity
public class SecurityConfig {
    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
    @Bean AuthenticationManager authenticationManager(CustomUserDetailsService users, PasswordEncoder passwords) {
        var provider = new DaoAuthenticationProvider(users); provider.setPasswordEncoder(passwords);
        return new ProviderManager(provider);
    }
    @Bean CorsConfigurationSource corsConfigurationSource(JwtProperties properties) {
        var config = new CorsConfiguration(); config.setAllowedOrigins(properties.allowedOrigins());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-CSRF-TOKEN")); config.setAllowCredentials(true);
        var source = new UrlBasedCorsConfigurationSource(); source.registerCorsConfiguration("/api/**", config); return source;
    }
    @Bean SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAccountConverter converter, SecurityErrors errors, JwtProperties properties) throws Exception {
        var csrf = new CookieCsrfTokenRepository(); csrf.setCookieName("CHAPTER-CSRF"); csrf.setHeaderName("X-CSRF-TOKEN");
        csrf.setCookieCustomizer(cookie -> cookie.httpOnly(true).secure(properties.cookieSecure()).sameSite(properties.cookieSameSite()).path("/api/auth"));
        var csrfHandler = new CsrfTokenRequestAttributeHandler();
        return http.cors(cors -> {}).sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(config -> config.csrfTokenRepository(csrf).csrfTokenRequestHandler(csrfHandler)
                        .requireCsrfProtectionMatcher(request -> request.getMethod().equals("POST") && List.of("/api/auth/login", "/api/auth/refresh", "/api/auth/logout").contains(request.getServletPath())))
                .formLogin(form -> form.disable()).httpBasic(basic -> basic.disable()).logout(logout -> logout.disable())
                .authorizeHttpRequests(auth -> auth
                    .dispatcherTypeMatchers(jakarta.servlet.DispatcherType.ERROR).permitAll()
                    .requestMatchers(HttpMethod.GET, "/actuator/health", "/api/auth/csrf").permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/refresh", "/api/auth/logout").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/auth/me").authenticated()
                    .requestMatchers(HttpMethod.PUT, "/api/auth/password").authenticated()
                    .requestMatchers(HttpMethod.GET, "/api/users").hasAuthority("ACCOUNT_ADMIN")
                    .requestMatchers(HttpMethod.POST, "/api/users").hasAuthority("ACCOUNT_ADMIN")
                    .requestMatchers(HttpMethod.PUT, "/api/users/*").hasAuthority("ACCOUNT_ADMIN")
                    .requestMatchers(HttpMethod.GET, "/api/events", "/api/events/*").hasAnyRole("COMMITTEE_MEMBER", "VOLUNTEER", "FINANCE_TEAM_LEAD", "LOGISTICS_TEAM_LEAD")
                    .requestMatchers(HttpMethod.GET,"/api/events/*/members","/api/events/*/members/options").hasRole("COMMITTEE_MEMBER")
                    .requestMatchers(HttpMethod.PUT,"/api/events/*/members/*","/api/events/*/purchase-groups/*/owner").hasRole("COMMITTEE_MEMBER")
                    .requestMatchers(HttpMethod.DELETE,"/api/events/*/members/*","/api/events/*/purchase-groups/*/owner").hasRole("COMMITTEE_MEMBER")
                    .requestMatchers(HttpMethod.GET,"/api/events/*/expenses","/api/events/*/incomes","/api/events/*/income-sources").hasAnyRole("COMMITTEE_MEMBER","FINANCE_TEAM_LEAD")
                    .requestMatchers(HttpMethod.POST,"/api/events/*/expenses","/api/events/*/incomes").hasAnyRole("COMMITTEE_MEMBER","FINANCE_TEAM_LEAD")
                    .requestMatchers(HttpMethod.PUT,"/api/events/*/expenses/*","/api/events/*/incomes/*").hasAnyRole("COMMITTEE_MEMBER","FINANCE_TEAM_LEAD")
                    .requestMatchers(HttpMethod.POST,"/api/events/*/income-sources").hasRole("COMMITTEE_MEMBER")
                    .requestMatchers(HttpMethod.GET,"/api/events/*/purchase-groups").hasAnyRole("COMMITTEE_MEMBER","LOGISTICS_TEAM_LEAD")
                    .requestMatchers(HttpMethod.POST,"/api/events/*/purchase-groups/*/quotations").hasAnyRole("COMMITTEE_MEMBER","LOGISTICS_TEAM_LEAD")
                    .requestMatchers(HttpMethod.PUT,"/api/events/*/purchase-groups/*/quotations/*").hasAnyRole("COMMITTEE_MEMBER","LOGISTICS_TEAM_LEAD")
                    .requestMatchers(HttpMethod.DELETE,"/api/events/*/purchase-groups/*/quotations/*").hasAnyRole("COMMITTEE_MEMBER","LOGISTICS_TEAM_LEAD")
                    .requestMatchers(HttpMethod.POST, "/api/events").hasRole("COMMITTEE_MEMBER")
                    .requestMatchers(HttpMethod.GET, "/api/events/*/budget", "/api/events/*/expenses", "/api/events/*/incomes", "/api/events/*/purchase-groups").hasRole("COMMITTEE_MEMBER")
                    .requestMatchers(HttpMethod.POST, "/api/events/*/budget", "/api/events/*/expenses", "/api/events/*/incomes", "/api/events/*/purchase-groups", "/api/events/*/purchase-groups/*/quotations").hasRole("COMMITTEE_MEMBER")
                    .requestMatchers(HttpMethod.PUT, "/api/events/*/budget/*", "/api/events/*/purchase-groups/*/quotations/*", "/api/events/*/purchase-groups/*/selection/*").hasRole("COMMITTEE_MEMBER")
                    .requestMatchers(HttpMethod.DELETE, "/api/events/*/budget/*", "/api/events/*/purchase-groups/*/quotations/*", "/api/events/*/purchase-groups/*/selection").hasRole("COMMITTEE_MEMBER")
                    .anyRequest().denyAll())
                .oauth2ResourceServer(resource -> resource.jwt(jwt -> jwt.jwtAuthenticationConverter(converter))
                    .authenticationEntryPoint((req,res,ex) -> errors.write(res,401)).accessDeniedHandler((req,res,ex) -> errors.write(res,403)))
                .exceptionHandling(config -> config.authenticationEntryPoint((req,res,ex) -> errors.write(res,401)).accessDeniedHandler((req,res,ex) -> errors.write(res,403)))
                .build();
    }
}
