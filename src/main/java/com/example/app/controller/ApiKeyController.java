package com.example.app.controller;

import com.example.app.security.ApiKey;
import com.example.app.security.ApiKeyRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/api-keys")
@Tag(name = "API Keys")
public class ApiKeyController {
  private final ApiKeyRepository repository;

  @Value("${admin.api-key}")
  private String configuredKey;

  public ApiKeyController(ApiKeyRepository repository) {
    this.repository = repository;
  }

  private void verify(String adminKey) {
    if (adminKey == null
        || !MessageDigest.isEqual(
            adminKey.getBytes(StandardCharsets.UTF_8),
            configuredKey.getBytes(StandardCharsets.UTF_8)))
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
  }

  private String hash(String raw) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException(exception);
    }
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Create API key")
  public Map<String, Object> create(
      @RequestHeader(value = "X-Admin-Key", required = false) String adminKey,
      @RequestBody Map<String, String> body) {
    verify(adminKey);
    String raw = UUID.randomUUID() + "-" + UUID.randomUUID();
    ApiKey key = new ApiKey();
    key.setName(body.getOrDefault("name", "api-client"));
    key.setKeyHash(hash(raw));
    key = repository.save(key);
    return Map.of("id", key.getId(), "name", key.getName(), "apiKey", raw);
  }

  @GetMapping
  @Operation(summary = "List API keys")
  public List<Map<String, Object>> list(
      @RequestHeader(value = "X-Admin-Key", required = false) String adminKey) {
    verify(adminKey);
    return repository.findAll().stream()
        .<Map<String, Object>>map(
            x -> Map.of("id", x.getId(), "name", x.getName(), "active", x.isActive()))
        .collect(Collectors.toList());
  }

  @PostMapping("/{id}/revoke")
  @Operation(summary = "Revoke API key")
  public Map<String, Object> revoke(
      @RequestHeader(value = "X-Admin-Key", required = false) String adminKey,
      @PathVariable Long id) {
    verify(adminKey);
    ApiKey key =
        repository
            .findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    key.setActive(false);
    repository.save(key);
    return Map.of("id", key.getId(), "active", false);
  }
}
