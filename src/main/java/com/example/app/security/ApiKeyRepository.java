package com.example.app.security;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApiKeyRepository extends JpaRepository<ApiKey, Long> {

  Optional<ApiKey> findByKeyHashAndActiveTrue(String keyHash);
}
