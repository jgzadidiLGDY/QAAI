# Phase 19 Plan: Channel-Neutral Scenario Model

## Summary

Phase 19 reframes the project from a voice-only QA tool into a voice-first,
channel-neutral AI agent testing framework.

The implementation should not add a second real channel yet. It should clarify
the shared platform concepts so voice remains the first adapter and text chat
has an obvious place to fit in Phase 20.

## Goal

Separate what scenario is being tested from how the interaction is executed.

## Implementation Plan

1. Audit voice-specific terminology in core docs and code.
2. Define channel-neutral terms:
   - interaction
   - channel
   - participant
   - turn
   - local run id
   - external channel id
3. Identify which current concepts remain voice-specific:
   - Retell outbound call
   - target phone number
   - recording URL
   - audio artifact
   - Retell call id
4. Update contracts and architecture docs to describe the channel boundary.
5. Make only narrow code changes if needed to prevent new platform docs from
   contradicting runtime names.
6. Preserve all current voice commands, artifact paths, and tests.
7. Add focused tests only for any code-level contract changes.

## Likely Files

- `README.md`
- `docs/project_specs.md`
- `docs/architecture.md`
- `docs/artifacts_model.md`
- `docs/result_contracts.md`
- `docs/run-lifecycle.md`
- `docs/operator-guide.md`
- `AI_native_builder_journal.md`
- possibly `src/main/java/com/qaai/runner/*`
- possibly `src/main/java/com/qaai/artifacts/*`
- possibly `src/main/java/com/qaai/scenario/*`

## Tests

If Phase 19 remains documentation and naming-boundary work:

```powershell
.\gradlew test
```

If code-level abstractions are introduced:

```powershell
.\gradlew test --tests "*ScenarioRunnerCommandTest" --tests "*Artifact*"
.\gradlew test
```

## Assumptions

- Voice remains the first supported real execution channel.
- Existing `call_id` values continue to identify local artifact bundles for
  backward compatibility.
- Retell-specific identifiers stay in metadata as channel-specific external ids.
- Text chat is the Phase 20 proving channel, not part of Phase 19 runtime work.
- Project Silver extraction rules are secondary. They may help shape future
  commits into clean task candidates, but they do not define the product
  direction for this project.

## Risks

- Over-generalizing too early could make the code harder to understand.
- Renaming too much at once could break artifact compatibility.
- Leaving all voice terminology unchanged could make Phase 20 duplicate the
  runner and artifact pipeline.
- A vague abstraction could hide real channel differences such as audio,
  session state, email threading, or browser evidence.

## Scope Boundaries

In scope:

- channel-neutral documentation
- explicit adapter boundary
- compatibility-preserving terminology cleanup
- voice and text as concrete planning examples
- tests for any touched runtime behavior

Out of scope:

- real text chat execution
- email-agent execution
- web-agent execution
- replacing Retell integration
- broad package renames
- artifact migrations
- AI-owned workflow control or pass/fail decisions

## Proposed Phase 20 Follow-Up

Phase 20 should add a small text chat runner prototype. It should prove that the
same scenario framework can produce a text interaction transcript and feed the
existing artifact, review, evaluation, and report pipeline without creating a
parallel system.
