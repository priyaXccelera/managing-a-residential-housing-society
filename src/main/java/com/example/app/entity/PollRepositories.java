package com.example.app.entity;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface NoticeRepository extends JpaRepository<Notice, Long> {
  List<Notice> findByExpiredTrueOrderByPostedDateDesc();

  List<Notice> findByCategoryAndExpiredFalseOrderByPostedDateDesc(NoticeCategory category);

  List<Notice> findByExpiredFalseOrderByPostedDateDesc();
}

interface PollRepository extends JpaRepository<Poll, Long> {}

interface PollOptionRepository extends JpaRepository<PollOption, Long> {}

interface VoteRepository extends JpaRepository<Vote, Long> {
  boolean existsByResidentIdAndPollId(Long residentId, Long pollId);

  List<Vote> findByPollId(Long pollId);

  void deleteByPollId(Long pollId);

  List<Vote> findByResidentIdOrderByTimestampDesc(Long residentId);
}
