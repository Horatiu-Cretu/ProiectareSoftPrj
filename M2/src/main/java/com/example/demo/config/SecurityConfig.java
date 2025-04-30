package com.example.demo.config;

import org.springframework.beans.factory.annotation.Autowired; // Import Autowired
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter; // Import standard filter

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // Inject the custom JWT Authentication Filter
    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter; // Make sure this matches the class name

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Disable CSRF (common for stateless APIs)
                .csrf(csrf -> csrf.disable())
                // Configure authorization rules
                .authorizeHttpRequests(authz -> authz
                        // Explicitly PERMIT public GET requests and OPTIONS
                        .requestMatchers(HttpMethod.GET, "/api/posts/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/comments/**").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // Any other request under /api/ must be authenticated
                        .requestMatchers("/api/**").authenticated() // Original, correct rule
                        // Other requests (if any, outside /api/) can be permitted or denied as needed
                        .anyRequest().permitAll() // Permit others like /error endpoint
                )
                // Configure session management to be stateless (essential for JWT)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // *** ADD THE CUSTOM FILTER HERE ***
                // Add the JwtAuthenticationFilter before the standard UsernamePasswordAuthenticationFilter
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}