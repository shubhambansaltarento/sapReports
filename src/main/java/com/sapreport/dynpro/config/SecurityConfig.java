package com.sapreport.dynpro.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * TEMPORARY: permits every request. Spring Security's default lockdown is
 * pulled in transitively by the oauth2-client/oauth2-resource-server
 * starters already in the pom, and otherwise blocks everything (including
 * Swagger UI) behind HTTP Basic. Real platform auth is JWT bearer, issued
 * by dynpro after business-token verification (see docs/spec/jwt-auth.md),
 * and is explicitly deferred — replace this with that filter chain once it
 * lands, rather than adding onto it.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }
}
