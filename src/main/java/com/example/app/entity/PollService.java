package com.example.app.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class PollService {
  private final NoticeRepository notices;
  private final PollRepository polls;
  private final PollOptionRepository options;
  private final VoteRepository votes;
  private final ResidentRepository residents;

  public PollService(
      NoticeRepository notices,
      PollRepository polls,
      PollOptionRepository options,
      VoteRepository votes,
      ResidentRepository residents) {
    this.notices = notices;
    this.polls = polls;
    this.options = options;
    this.votes = votes;
    this.residents = residents;
  }

  public Map<String, Object> createNotice(Map<String, Object> body) {
    Notice notice = new Notice();
    notice.title = required(body, "title");
    notice.description = required(body, "description");
    notice.category = category(required(body, "category"));
    notice.expiryDate = optionalTime(body, "expiryDate");
    if (notice.expiryDate != null && !notice.expiryDate.isAfter(notice.postedDate)) {
      throw badRequest("expiryDate must be after postedDate");
    }
    return notice(notices.save(notice));
  }

  public Map<String, Object> updateNotice(Long id, Map<String, Object> body) {
    Notice notice = noticeEntity(id);
    if (body.containsKey("title")) notice.title = required(body, "title");
    if (body.containsKey("description")) notice.description = required(body, "description");
    if (body.containsKey("category")) notice.category = category(required(body, "category"));
    if (body.containsKey("expiryDate")) {
      notice.expiryDate = optionalTime(body, "expiryDate");
      if (notice.expiryDate != null && !notice.expiryDate.isAfter(notice.postedDate)) {
        throw badRequest("expiryDate must be after postedDate");
      }
    }
    return notice(notices.save(notice));
  }

  public Map<String, Object> expireNotice(Long id) {
    Notice notice = noticeEntity(id);
    notice.expired = true;
    notice.expiryDate = LocalDateTime.now();
    return notice(notices.save(notice));
  }

  @Transactional(readOnly = true)
  public Map<String, Object> activeNotices(String category, int offset, int limit) {
    List<Notice> source =
        category == null || category.isBlank()
            ? notices.findByExpiredFalseOrderByPostedDateDesc()
            : notices.findByCategoryAndExpiredFalseOrderByPostedDateDesc(category(category));
    LocalDateTime now = LocalDateTime.now();
    return page(
        source.stream()
            .filter(n -> n.expiryDate == null || n.expiryDate.isAfter(now))
            .map(this::notice),
        offset,
        limit);
  }

  @Transactional(readOnly = true)
  public Map<String, Object> expiredNotices(int offset, int limit) {
    LocalDateTime now = LocalDateTime.now();
    return page(
        notices.findAll().stream()
            .filter(n -> n.expired || (n.expiryDate != null && !n.expiryDate.isAfter(now)))
            .sorted(Comparator.comparing(n -> n.postedDate, Comparator.reverseOrder()))
            .map(this::notice),
        offset,
        limit);
  }

  public Map<String, Object> createPoll(Map<String, Object> body) {
    Poll poll = new Poll();
    applyPollFields(poll, body);
    attachOptions(poll, optionTexts(body));
    if (body.containsKey("noticeId")) poll.notice = noticeEntity(longValue(body, "noticeId"));
    return poll(polls.save(poll));
  }

  public Map<String, Object> updatePoll(Long id, Map<String, Object> body) {
    Poll poll = pollEntity(id);
    if (!LocalDateTime.now().isBefore(poll.votingStartTime)) {
      throw badRequest("poll cannot be edited after voting starts");
    }
    applyPollFields(poll, body);
    if (body.containsKey("options")) {
      poll.options.clear();
      attachOptions(poll, optionTexts(body));
    }
    if (body.containsKey("noticeId")) poll.notice = noticeEntity(longValue(body, "noticeId"));
    return poll(polls.save(poll));
  }

  public Map<String, Object> cancelPoll(Long id, Map<String, Object> body) {
    Poll poll = pollEntity(id);
    if (poll.cancelled) throw badRequest("poll is already cancelled");
    LocalDateTime now = LocalDateTime.now();
    if (!now.isBefore(poll.votingStartTime)) {
      poll.cancellationReason = required(body, "reason");
      votes.deleteByPollId(poll.id);
      poll.options.forEach(option -> option.voteCount = 0);
    }
    poll.cancelled = true;
    return poll(polls.save(poll));
  }

  public Map<String, Object> castVote(Long pollId, Map<String, Object> body) {
    Poll poll = pollEntity(pollId);
    LocalDateTime now = LocalDateTime.now();
    if (poll.cancelled) throw badRequest("poll is cancelled");
    if (!now.isAfter(poll.votingStartTime)) throw badRequest("poll has not started");
    if (!now.isBefore(poll.votingEndTime)) throw badRequest("poll has ended");
    Resident resident = resident(longValue(body, "residentId"));
    if (!resident.active || resident.flat == null || !resident.flat.active) {
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN, "only active residents linked to a flat can vote");
    }
    if (votes.existsByResidentIdAndPollId(resident.id, poll.id)) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "resident has already voted in this poll");
    }
    PollOption option = option(longValue(body, "pollOptionId"));
    if (!option.poll.id.equals(poll.id))
      throw badRequest("pollOptionId does not belong to this poll");
    Vote vote = new Vote();
    vote.resident = resident;
    vote.poll = poll;
    vote.chosenPollOption = option;
    votes.save(vote);
    option.voteCount++;
    options.save(option);
    return Map.of("id", vote.id, "pollId", poll.id, "timestamp", vote.timestamp.toString());
  }

  @Transactional(readOnly = true)
  public Map<String, Object> listPolls(String status, String quorumStatus, int offset, int limit) {
    LocalDateTime now = LocalDateTime.now();
    List<Map<String, Object>> result = new ArrayList<>();
    for (Poll poll : polls.findAll()) {
      String computedStatus = status(poll, now);
      String computedQuorum = quorumStatus(poll, now);
      if ((status == null || status.isBlank() || computedStatus.equalsIgnoreCase(status))
          && (quorumStatus == null
              || quorumStatus.isBlank()
              || computedQuorum.equalsIgnoreCase(quorumStatus))) {
        result.add(poll(poll));
      }
    }
    return page(result.stream(), offset, limit);
  }

  @Transactional(readOnly = true)
  public Map<String, Object> results(Long pollId) {
    Poll poll = pollEntity(pollId);
    if (poll.cancelled) throw badRequest("poll is cancelled");
    if (!LocalDateTime.now().isAfter(poll.votingEndTime)) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "poll results are not yet available");
    }
    long totalVotes = votes.findByPollId(poll.id).size();
    long activeResidents =
        residents.findAll().stream()
            .filter(r -> r.active && r.flat != null && r.flat.active)
            .count();
    double turnout = activeResidents == 0 ? 0 : totalVotes * 100.0 / activeResidents;
    boolean quorumMet = turnout >= poll.minimumQuorumPercentage;
    List<Map<String, Object>> optionBreakdown =
        poll.options.stream()
            .map(
                o ->
                    Map.<String, Object>of(
                        "id",
                        o.id,
                        "optionText",
                        o.optionText,
                        "voteCount",
                        o.voteCount,
                        "percentage",
                        totalVotes == 0 ? 0.0 : o.voteCount * 100.0 / totalVotes))
            .collect(Collectors.toList());
    Map<String, Object> response = new LinkedHashMap<>();
    response.put("pollId", poll.id);
    response.put("totalVotes", totalVotes);
    response.put("activeResidentCount", activeResidents);
    response.put("turnoutPercentage", turnout);
    response.put("minimumQuorumPercentage", poll.minimumQuorumPercentage);
    response.put("quorumStatus", quorumMet ? "MET" : "NOT_MET");
    response.put("options", optionBreakdown);
    if (!quorumMet) {
      response.put("outcome", "INVALID");
      response.put("winningOptions", List.of());
    } else {
      long highest = poll.options.stream().mapToLong(o -> o.voteCount).max().orElse(0);
      List<String> winners =
          poll.options.stream()
              .filter(o -> o.voteCount == highest)
              .map(o -> o.optionText)
              .collect(Collectors.toList());
      response.put("outcome", winners.size() > 1 ? "TIE" : "WINNER");
      response.put("winningOptions", winners);
    }
    return response;
  }

  @Transactional(readOnly = true)
  public Map<String, Object> votingHistory(Long residentId, int offset, int limit) {
    resident(residentId);
    return page(
        votes.findByResidentIdOrderByTimestampDesc(residentId).stream()
            .map(
                v ->
                    Map.<String, Object>of(
                        "pollId", v.poll.id, "timestamp", v.timestamp.toString())),
        offset,
        limit);
  }

  private void applyPollFields(Poll poll, Map<String, Object> body) {
    if (body.containsKey("question") || poll.question == null)
      poll.question = required(body, "question");
    if (body.containsKey("votingStartTime") || poll.votingStartTime == null)
      poll.votingStartTime = requiredTime(body, "votingStartTime");
    if (body.containsKey("votingEndTime") || poll.votingEndTime == null)
      poll.votingEndTime = requiredTime(body, "votingEndTime");
    if (body.containsKey("minimumQuorumPercentage") || poll.minimumQuorumPercentage == 0) {
      poll.minimumQuorumPercentage = doubleValue(body, "minimumQuorumPercentage");
    }
    if (!poll.votingEndTime.isAfter(poll.votingStartTime))
      throw badRequest("votingEndTime must be after votingStartTime");
    if (poll.minimumQuorumPercentage < 0 || poll.minimumQuorumPercentage > 100)
      throw badRequest("minimumQuorumPercentage must be between 0 and 100");
  }

  @SuppressWarnings("unchecked")
  private List<String> optionTexts(Map<String, Object> body) {
    Object raw = body.get("options");
    if (!(raw instanceof List<?>)) throw badRequest("options must contain at least two entries");
    List<String> texts =
        ((List<Object>) raw)
            .stream().map(String::valueOf).filter(s -> !s.isBlank()).collect(Collectors.toList());
    if (texts.size() < 2) throw badRequest("poll must contain at least two options");
    return texts;
  }

  private void attachOptions(Poll poll, List<String> texts) {
    for (String text : texts) {
      PollOption option = new PollOption();
      option.poll = poll;
      option.optionText = text;
      poll.options.add(option);
    }
  }

  private String status(Poll poll, LocalDateTime now) {
    if (poll.cancelled) return "CANCELLED";
    if (!now.isAfter(poll.votingStartTime)) return "UPCOMING";
    return now.isBefore(poll.votingEndTime) ? "ACTIVE" : "CLOSED";
  }

  private String quorumStatus(Poll poll, LocalDateTime now) {
    if (!"CLOSED".equals(status(poll, now))) return "PENDING";
    long count = votes.findByPollId(poll.id).size();
    long eligible =
        residents.findAll().stream()
            .filter(r -> r.active && r.flat != null && r.flat.active)
            .count();
    return eligible > 0 && count * 100.0 / eligible >= poll.minimumQuorumPercentage
        ? "MET"
        : "NOT_MET";
  }

  private Map<String, Object> notice(Notice notice) {
    Map<String, Object> response = new LinkedHashMap<>();
    response.put("id", notice.id);
    response.put("title", notice.title);
    response.put("description", notice.description);
    response.put("category", notice.category.name());
    response.put("postedDate", notice.postedDate.toString());
    response.put("expiryDate", notice.expiryDate == null ? null : notice.expiryDate.toString());
    response.put(
        "expired",
        notice.expired
            || (notice.expiryDate != null && !notice.expiryDate.isAfter(LocalDateTime.now())));
    return response;
  }

  private Map<String, Object> poll(Poll poll) {
    Map<String, Object> response = new LinkedHashMap<>();
    response.put("id", poll.id);
    response.put("question", poll.question);
    response.put("votingStartTime", poll.votingStartTime.toString());
    response.put("votingEndTime", poll.votingEndTime.toString());
    response.put("minimumQuorumPercentage", poll.minimumQuorumPercentage);
    response.put("noticeId", poll.notice == null ? null : poll.notice.id);
    response.put("status", status(poll, LocalDateTime.now()));
    response.put("cancelled", poll.cancelled);
    response.put(
        "cancellationReason", poll.cancellationReason == null ? null : poll.cancellationReason);
    response.put(
        "options",
        poll.options.stream()
            .map(o -> Map.of("id", o.id, "optionText", o.optionText))
            .collect(Collectors.toList()));
    return response;
  }

  private Map<String, Object> page(
      java.util.stream.Stream<Map<String, Object>> stream, int offset, int limit) {
    if (offset < 0 || limit < 1 || limit > 100)
      throw badRequest("offset must be non-negative and limit must be between 1 and 100");
    List<Map<String, Object>> all = stream.collect(Collectors.toList());
    int from = Math.min(offset, all.size());
    int to = Math.min(from + limit, all.size());
    return Map.of(
        "total", all.size(), "offset", offset, "limit", limit, "items", all.subList(from, to));
  }

  private Notice noticeEntity(Long id) {
    return notices.findById(id).orElseThrow(() -> notFound("notice not found"));
  }

  private Poll pollEntity(Long id) {
    return polls.findById(id).orElseThrow(() -> notFound("poll not found"));
  }

  private PollOption option(Long id) {
    return options.findById(id).orElseThrow(() -> notFound("poll option not found"));
  }

  private Resident resident(Long id) {
    return residents.findById(id).orElseThrow(() -> notFound("resident not found"));
  }

  private String required(Map<String, Object> body, String key) {
    Object value = body.get(key);
    if (value == null || String.valueOf(value).isBlank()) throw badRequest(key + " is required");
    return String.valueOf(value);
  }

  private Long longValue(Map<String, Object> body, String key) {
    try {
      return Long.valueOf(required(body, key));
    } catch (NumberFormatException e) {
      throw badRequest(key + " must be a number");
    }
  }

  private double doubleValue(Map<String, Object> body, String key) {
    try {
      return Double.parseDouble(required(body, key));
    } catch (NumberFormatException e) {
      throw badRequest(key + " must be numeric");
    }
  }

  private LocalDateTime requiredTime(Map<String, Object> body, String key) {
    try {
      return LocalDateTime.parse(required(body, key));
    } catch (RuntimeException e) {
      throw badRequest(key + " must be ISO-8601 local date-time");
    }
  }

  private LocalDateTime optionalTime(Map<String, Object> body, String key) {
    Object value = body.get(key);
    return value == null || String.valueOf(value).isBlank() ? null : requiredTime(body, key);
  }

  private NoticeCategory category(String value) {
    try {
      return NoticeCategory.valueOf(value.toUpperCase());
    } catch (IllegalArgumentException e) {
      throw badRequest("category must be GENERAL, URGENT, or EVENT");
    }
  }

  private ResponseStatusException badRequest(String message) {
    return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
  }

  private ResponseStatusException notFound(String message) {
    return new ResponseStatusException(HttpStatus.NOT_FOUND, message);
  }
}
