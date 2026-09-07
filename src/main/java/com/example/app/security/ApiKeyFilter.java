package com.example.app.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class ApiKeyFilter extends OncePerRequestFilter {

  private final ApiKeyRepository apiKeyRepository;

  public ApiKeyFilter(ApiKeyRepository apiKeyRepository) {
    this.apiKeyRepository = apiKeyRepository;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String rawKey = request.getHeader("X-API-Key");
    if (rawKey != null && SecurityContextHolder.getContext().getAuthentication() == null) {
      apiKeyRepository
          .findByKeyHashAndActiveTrue(hash(rawKey))
          .ifPresent(
              apiKey -> {
                apiKey.setLastUsedAt(Instant.now());
                apiKeyRepository.save(apiKey);
                UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                        apiKey.getName(),
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_API_CLIENT")));
                SecurityContextHolder.getContext().setAuthentication(authentication);
              });
    }
    filterChain.doFilter(request, response);
  }

  private String hash(String value) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException(exception);
    }
  }
}
