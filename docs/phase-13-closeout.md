# Phase 13 Closeout

Phase 13 made short or shallow conversations visible to human reviewers without
turning depth signals into automated pass/fail decisions.

## Completed

- Captured Retell `duration_ms` as `call_duration_seconds` in updated
  `metadata.json`.
- Added deterministic conversation-depth review signals to `observations.md`.
- Distinguished unknown duration from short, typical, and long duration.
- Added advisory checks for turn counts, patient goal statement,
  workflow-specific receptionist questions, and confirmation or next-step cues.
- Tightened next-step evidence matching so words like `already` do not count as
  `ready` evidence.
- Added a post-capture CLI hint that points operators to
  `--review-conversation` so refreshed depth observations are easier to find.
- Kept older metadata readable when `call_duration_seconds` is absent.
- Preserved human-owned review and AI-assisted analysis boundaries.

## Local Usage

After capture:

```powershell
.\gradlew bootRun --args="--review-conversation --call-id=<local_call_id>"
```

The review writes:

```text
outputs/{call_id}/observations.md
```

## Live Calibration

One authorized live Retell call was placed on May 27, 2026 for the appointment
rescheduling scenario.

```text
call_id: call_20260527_133343_cd7d9a47
retell_call_id: call_cc84b5fabee359005c74e4f6b65
call_duration_seconds: 131
turn_count: 27 total, 13 patient, 14 receptionist
duration_signal: typical
```

The first capture happened while Retell still reported the call as ongoing, so
duration was unknown. A later capture after processing completed recorded the
duration and audio.

## Still Out Of Scope

- Automated pass/fail decisions.
- Provider-specific retry automation.
- Batch depth review.
- Issue clustering across runs.
- Clinical quality judgment.
- Scenario coverage expansion, which remains Phase 14.

## Tests

```powershell
.\gradlew test --tests "*ConversationQualityReviewServiceTest" --tests "*ScenarioRunnerCommandTest" --tests "*ArtifactCaptureServiceTest"
.\gradlew test
```

## Silver Notes

Behavior commit:

- Base commit: `379cdd2`
- Fix commit: `fcd2b7a`

The candidate behavior is conversation-depth review: captured Retell durations
are persisted when available, and review observations distinguish unknown,
short, typical, and long calls while citing transcript-backed depth signals.
