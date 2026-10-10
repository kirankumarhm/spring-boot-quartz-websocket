package com.enterprise.quartz.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security 6+ configuration enforcing Role-Based Access Control (RBAC).
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_OPERATOR = "OPERATOR";
    public static final String ROLE_VIEWER = "VIEWER";

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Public OpenAPI & Documentation Endpoints
                        .requestMatchers(
                                "/",
                                "/dashboard",
                                "/dashboard.html",
                                "/ws-quartz/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/actuator/health",
                                "/actuator/info"
                        ).permitAll()

                        // Read-only queries for viewers
                        .requestMatchers(HttpMethod.GET, "/api/v1/jobs/**").hasAnyRole(ROLE_VIEWER, ROLE_OPERATOR, ROLE_ADMIN)

                        // Operations: pause, resume, triggerNow
                        .requestMatchers(HttpMethod.POST, "/api/v1/jobs/*/*/pause").hasAnyRole(ROLE_OPERATOR, ROLE_ADMIN)
                        .requestMatchers(HttpMethod.POST, "/api/v1/jobs/*/*/resume").hasAnyRole(ROLE_OPERATOR, ROLE_ADMIN)
                        .requestMatchers(HttpMethod.POST, "/api/v1/jobs/*/*/trigger").hasAnyRole(ROLE_OPERATOR, ROLE_ADMIN)

                        // Administrative endpoints: create, edit, delete, global pause/resume
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/jobs/history").hasAnyRole(ROLE_OPERATOR, ROLE_ADMIN)
                        .requestMatchers(HttpMethod.POST, "/api/v1/jobs").hasRole(ROLE_ADMIN)
                        .requestMatchers(HttpMethod.PUT, "/api/v1/jobs/**").hasRole(ROLE_ADMIN)
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/jobs/**").hasRole(ROLE_ADMIN)
                        .requestMatchers(HttpMethod.POST, "/api/v1/jobs/pause-all").hasRole(ROLE_ADMIN)
                        .requestMatchers(HttpMethod.POST, "/api/v1/jobs/resume-all").hasRole(ROLE_ADMIN)

                        .anyRequest().authenticated()
                )
                .httpBasic(Customizer.withDefaults());

        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder passwordEncoder) {
        UserDetails admin = User.builder()
                .username("admin")
                .password(passwordEncoder.encode("admin123"))
                .roles(ROLE_ADMIN, ROLE_OPERATOR, ROLE_VIEWER)
                .build();

        UserDetails operator = User.builder()
                .username("operator")
                .password(passwordEncoder.encode("operator123"))
                .roles(ROLE_OPERATOR, ROLE_VIEWER)
                .build();

        UserDetails viewer = User.builder()
                .username("viewer")
                .password(passwordEncoder.encode("viewer123"))
                .roles(ROLE_VIEWER)
                .build();

        return new InMemoryUserDetailsManager(admin, operator, viewer);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
