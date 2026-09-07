package com.example.app.config;

import com.example.app.security.ApiKeyFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

  private final ApiKeyFilter apiKeyFilter;

  public SecurityConfig(ApiKeyFilter apiKeyFilter) {
    this.apiKeyFilter = apiKeyFilter;
  }

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http.csrf(csrf -> csrf.disable())
        .cors(Customizer.withDefaults())
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeRequests(
            auth ->
                auth.antMatchers(
                        "/api/v1/api-keys/**",
                        "/actuator/health",
                        "/docs/**",
                        "/api-docs/**",
                        "/swagger-ui/**")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .addFilterBefore(apiKeyFilter, UsernamePasswordAuthenticationFilter.class);
    return http.build();
  }
}
