package wemove.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import wemove.identity.security.DatabaseUserDetailsService;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;
import org.springframework.context.annotation.*;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.*;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.web.cors.*;

@Configuration
public class SecurityConfig {
    @Bean
    PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(12); }

    @Bean
    AuthenticationManager authenticationManager(DatabaseUserDetailsService details, PasswordEncoder encoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(details);
        provider.setPasswordEncoder(encoder);
        return new ProviderManager(provider);
    }

    @Bean
    HttpSessionSecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, ObjectMapper mapper) throws Exception {
        HttpSessionCsrfTokenRepository csrf = new HttpSessionCsrfTokenRepository();
        csrf.setHeaderName("X-CSRF-Token");
        http
            .cors(cors -> {})
            .csrf(config -> config.csrfTokenRepository(csrf))
            .formLogin(config -> config.disable())
            .httpBasic(config -> config.disable())
            .logout(config -> config.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/auth/csrf", "/api/v1/auth/registration-policy",
                    "/api/v1/auth/register", "/api/v1/auth/login", "/error").permitAll()
                .requestMatchers(org.springframework.http.HttpMethod.GET,
                    "/api/v1/products/**", "/api/v1/categories", "/api/v1/product-options",
                    "/api/v1/channels/**").permitAll()
                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated())
            .exceptionHandling(errors -> errors
                .authenticationEntryPoint((request, response, ex) -> writeProblem(
                    mapper, response, 401, "UNAUTHENTICATED", "请先登录。", request.getAttribute("requestId")))
                .accessDeniedHandler((request, response, ex) -> writeProblem(
                    mapper, response, 403, "FORBIDDEN", "您无权执行该操作。", request.getAttribute("requestId"))));
        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(WemoveProperties properties) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.asList(properties.security().allowedOrigins().split(",")));
        config.setAllowedMethods(java.util.List.of("GET", "POST", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(java.util.List.of("Content-Type", "X-CSRF-Token", "Idempotency-Key"));
        config.setExposedHeaders(java.util.List.of("X-Request-Id", "Idempotency-Replayed"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }

    private void writeProblem(ObjectMapper mapper, HttpServletResponse response, int status,
                              String code, String detail, Object requestId) throws java.io.IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        mapper.writeValue(response.getOutputStream(), java.util.Map.of(
            "type", "about:blank", "title", status == 401 ? "Unauthorized" : "Forbidden",
            "status", status, "detail", detail, "code", code,
            "requestId", requestId == null ? "" : requestId.toString()));
    }
}
