# Phase 18 Closeout

Phase 18 added structured multi-lens review over existing call artifacts.

## Completed

- Added `--multi-lens-review --call-id=<local_call_id>`.
- Added `com.qaai.review`.
- Added deterministic local multi-lens review.
- Added disabled multi-lens review mode.
- Added review provider configuration through `QAAI_REVIEW_PROVIDER=local|disabled`.
- Wrote `multi-lens-review.json` and `multi-lens-review.md` under
  `outputs/{call_id}/`.
- Added safety, consistency, patient realism, adversarial robustness, and
  workflow risk lens results.
- Validated transcript evidence for every concrete finding.
- Allowed explicit insufficient-evidence lens results without guessed findings.
- Updated metadata, manifest, and run index entries after successful review.
- Preserved human review and avoided autonomous multi-agent orchestration.

## Local Usage

```powershell
.\gradlew bootRun --args="--multi-lens-review --call-id=<local_call_id>"
```

Expected artifacts:

```text
outputs/{call_id}/multi-lens-review.json
outputs/{call_id}/multi-lens-review.md
```

## Configuration

```text
QAAI_REVIEW_PROVIDER=local
```

Use `QAAI_REVIEW_PROVIDER=disabled` when multi-lens review should fail clearly.

## Still Out Of Scope

- Autonomous agent fleets.
- Dynamic agent-to-agent coordination.
- AI-owned workflow control.
- AI-owned pass/fail decisions.
- Automatic scenario promotion.
- Automatic live-call execution.
- Mutation of source artifacts.
- Real patient data.

## Tests

```powershell
.\gradlew test --tests "*MultiLensReview*" --tests "*ScenarioRunnerCommandTest" --tests "*QaaiPropertiesTest" --tests "*ArtifactCompletenessCheckerTest"
.\gradlew test
```

## Silver Notes

Behavior commit:

- Structured multi-lens review workflow:
  - Base commit: TBD
  - Fix commit: TBD

The strongest Phase 18 candidate is the structured multi-lens review workflow
because it crosses CLI routing, provider configuration, artifact loading,
evidence validation, metadata/manifest/index updates, Markdown rendering, and
deterministic tests.
