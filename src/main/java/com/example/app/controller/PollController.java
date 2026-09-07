package com.example.app.controller;

import com.example.app.entity.PollService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import javax.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Notices and Polls")
public class PollController {
  private final PollService service;

  public PollController(PollService service) {
    this.service = service;
  }

  @PostMapping("/notices")
  @Operation(summary = "Create a notice")
  public ResponseEntity<Map<String, Object>> createNotice(
      @Valid @RequestBody Map<String, Object> body) {
    return ResponseEntity.status(HttpStatus.CREATED).body(service.createNotice(body));
  }

  @PutMapping("/notices/{id}")
  @Operation(summary = "Update a notice")
  public ResponseEntity<Map<String, Object>> updateNotice(
      @PathVariable Long id, @Valid @RequestBody Map<String, Object> body) {
    return ResponseEntity.ok(service.updateNotice(id, body));
  }

  @PostMapping("/notices/{id}/expire")
  @Operation(summary = "Expire a notice")
  public ResponseEntity<Map<String, Object>> expireNotice(@PathVariable Long id) {
    return ResponseEntity.ok(service.expireNotice(id));
  }

  @GetMapping("/notices")
  @Operation(summary = "List active notices")
  public ResponseEntity<Map<String, Object>> activeNotices(
      @RequestParam(required = false) String category,
      @RequestParam(defaultValue = "0") int offset,
      @RequestParam(defaultValue = "20") int limit) {
    return ResponseEntity.ok(service.activeNotices(category, offset, limit));
  }

  @GetMapping("/notices/expired")
  @Operation(summary = "List expired notice history")
  public ResponseEntity<Map<String, Object>> expiredNotices(
      @RequestParam(defaultValue = "0") int offset, @RequestParam(defaultValue = "20") int limit) {
    return ResponseEntity.ok(service.expiredNotices(offset, limit));
  }

  @PostMapping("/polls")
  @Operation(summary = "Create a poll")
  public ResponseEntity<Map<String, Object>> createPoll(
      @Valid @RequestBody Map<String, Object> body) {
    return ResponseEntity.status(HttpStatus.CREATED).body(service.createPoll(body));
  }

  @PutMapping("/polls/{id}")
  @Operation(summary = "Update a poll before voting starts")
  public ResponseEntity<Map<String, Object>> updatePoll(
      @PathVariable Long id, @Valid @RequestBody Map<String, Object> body) {
    return ResponseEntity.ok(service.updatePoll(id, body));
  }

  @PostMapping("/polls/{id}/cancel")
  @Operation(summary = "Cancel a poll")
  public ResponseEntity<Map<String, Object>> cancelPoll(
      @PathVariable Long id, @Valid @RequestBody Map<String, Object> body) {
    return ResponseEntity.ok(service.cancelPoll(id, body));
  }

  @PostMapping("/polls/{id}/votes")
  @Operation(summary = "Cast one vote in an active poll")
  public ResponseEntity<Map<String, Object>> vote(
      @PathVariable Long id, @Valid @RequestBody Map<String, Object> body) {
    return ResponseEntity.status(HttpStatus.CREATED).body(service.castVote(id, body));
  }

  @GetMapping("/polls")
  @Operation(summary = "List polls by lifecycle and quorum status")
  public ResponseEntity<Map<String, Object>> listPolls(
      @RequestParam(required = false) String status,
      @RequestParam(required = false) String quorumStatus,
      @RequestParam(defaultValue = "0") int offset,
      @RequestParam(defaultValue = "20") int limit) {
    return ResponseEntity.ok(service.listPolls(status, quorumStatus, offset, limit));
  }

  @GetMapping("/polls/{id}/results")
  @Operation(summary = "View poll result breakdown after voting ends")
  public ResponseEntity<Map<String, Object>> results(@PathVariable Long id) {
    return ResponseEntity.ok(service.results(id));
  }

  @GetMapping("/residents/{id}/voting-history")
  @Operation(summary = "View a resident voting history without vote choices")
  public ResponseEntity<Map<String, Object>> votingHistory(
      @PathVariable Long id,
      @RequestParam(defaultValue = "0") int offset,
      @RequestParam(defaultValue = "20") int limit) {
    return ResponseEntity.ok(service.votingHistory(id, offset, limit));
  }
}
