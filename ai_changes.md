COMMIT_MESSAGE: Add notices, polls, voting, and results reporting

## Features Added
- Notice creation, update, expiry, active category filtering, and expired-history pagination.
- Poll creation with validated timing, quorum, optional notice linkage, and at least two options.
- Pre-start poll editing and cancellation; post-start cancellation requires a reason and invalidates votes.
- Secure one-vote-per-active-flat-resident voting with strict start/end boundary enforcement.
- Post-end results reporting with turnout, recalculated active-resident quorum, option percentages, winners, ties, and invalid outcomes.
- Poll lifecycle/quorum filtering and resident voting history that never exposes selected options.

## Files Modified
- src/main/resources/application.properties — configured port 21126, fresh PostgreSQL JDBC URL, health exposure, and environment-backed admin key.
- src/main/java/com/example/app/controller/ApiKeyController.java — reads the configured admin key from the app.secret property.
- start.sh — defaults to port 21126.

## Files Added
- src/main/java/com/example/app/entity/PollModels.java — Notice, Poll, PollOption, Vote entities and notice category enum.
- src/main/java/com/example/app/entity/PollRepositories.java — persistence repositories for notices, polls, options, and votes.
- src/main/java/com/example/app/entity/PollService.java — notice, poll, vote, quorum, results, and privacy-safe history business rules.
- src/main/java/com/example/app/controller/PollController.java — /api/v1 notice and poll REST endpoints with offset/limit responses.
- ai_changes.md — implementation and build summary.

## Secrets Moved
- admin.api-key -> app.secret.admin-api-key

## DB URLs Resolved
- ${DB_URL} -> jdbc:postgresql://localhost:5432/gen_1e9802722e6b

## Compilation Result
PASSED — ./gradlew compileJava -q and ./gradlew bootJar -q completed successfully on Java 21 while honoring the Gradle Java 17 target.
