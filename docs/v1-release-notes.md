# v1 Release Notes

Voice AI QA Agent v1.0.0 is the first major reviewable milestone for the
project.

## Release Positioning

v1 is a local-first QA workflow for testing healthcare AI agents with synthetic
patient scenarios, reproducible artifacts, and advisory human-reviewed analysis.

It is not a production healthcare voice platform, clinical decision system, or
automatic pass/fail authority.

## Included Capabilities

- YAML scenario loading and validation.
- Deterministic dry runs.
- Local text-chat runs.
- Retell outbound call start.
- Retell artifact capture for transcripts, recordings when available, metadata,
  and manifests.
- Scenario-driven patient simulation prompts.
- Normalized transcript artifacts.
- Append-only run index and run inspection commands.
- Conversation-quality and depth observations.
- Pluggable advisory analysis with OpenAI, local, and disabled modes.
- Advisory evidence-linked evaluation dimensions.
- Static report generation over existing artifacts.
- AI-assisted scenario draft generation with human promotion boundary.
- Structured multi-lens advisory review.
- Channel metadata for voice and text runs.
- Agent-under-test profiles.
- Deterministic suite runs through local text-chat execution.

## Review Surface

Reviewers should start with:

- [README](../README.md)
- [v1 Review Guide](v1-review-guide.md)
- [Operator Guide](operator-guide.md)
- generated artifacts under `outputs/`
- generated static reports under `outputs/reports/`

## Deliberately Out Of Scope

- Database storage.
- Interactive web dashboard.
- Hosted deployment.
- Automatic pass/fail decisions.
- Live batch Retell execution by default.
- Automatic promotion of generated scenarios.
- Real patient data.

## Verification

The expected v1 verification command is:

```powershell
.\gradlew test
```

The expected local reviewer smoke command is:

```powershell
.\gradlew bootRun --args="--suite=suites/receptionist-smoke.yaml"
```
