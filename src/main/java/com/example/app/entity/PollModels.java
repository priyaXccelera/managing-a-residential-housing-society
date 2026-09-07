package com.example.app.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

@Entity
class Notice {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  Long id;

  String title;
  String description;

  @Enumerated(EnumType.STRING)
  NoticeCategory category;

  LocalDateTime postedDate = LocalDateTime.now();
  LocalDateTime expiryDate;
  boolean expired = false;
}

@Entity
@Table(name = "society_poll")
class Poll {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  Long id;

  String question;
  LocalDateTime votingStartTime;
  LocalDateTime votingEndTime;
  double minimumQuorumPercentage;
  boolean cancelled = false;
  String cancellationReason;

  @ManyToOne Notice notice;

  @OneToMany(mappedBy = "poll", cascade = CascadeType.ALL, orphanRemoval = true)
  List<PollOption> options = new ArrayList<>();
}

@Entity
class PollOption {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  Long id;

  String optionText;
  long voteCount = 0;

  @ManyToOne Poll poll;
}

@Entity
@Table(
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_vote_resident_poll",
            columnNames = {"resident_id", "poll_id"}))
class Vote {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  Long id;

  @ManyToOne Resident resident;

  @ManyToOne Poll poll;

  @ManyToOne PollOption chosenPollOption;

  LocalDateTime timestamp = LocalDateTime.now();
}

enum NoticeCategory {
  GENERAL,
  URGENT,
  EVENT
}
