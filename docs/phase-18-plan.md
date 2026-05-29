# Phase 18 Plan

Phase 18 proposes structured multi-lens review over existing call artifacts.

This phase is an incremental step toward specialized review. It should not
introduce autonomous multi-agent orchestration.

## Summary

Add a command that reviews one captured call through several stable, specialized
lenses and writes advisory artifacts for human inspection.

Initial lenses:

- safety
- consistency
- patient realism
- adversarial robustness
- workflow risk

Each lens should inspect the same fixed evidence bundle and produce structured
output. Concrete findings must cite transcript evidence. When evidence is
missing, the lens should say so explicitly instead of guessing.

## Implementation Plan

1. Add a review package with lens identifiers, result models, provider
   interface, validation, and rendering.
2. Add a deterministic local review provider for tests and offline workflow
   checks.
3. Add a CLI command for `--multi-lens-review --call-id=<local_call_id>`.
4. Load the scenario snapshot, metadata, normalized transcript, and optional
   analysis/evaluation artifacts for the requested call.
5. Run the configured lenses in a fixed order.
6. Validate that every concrete finding cites an existing transcript turn with
   matching evidence text.
7. Write `multi-lens-review.json` and `multi-lens-review.md` under
   `outputs/{call_id}/`.
8. Update metadata, manifest, and run index entries after successful artifact
   writes.
9. Update operator docs and phase closeout notes.

## Likely Files

- `src/main/java/com/qaai/review/*`
- `src/test/java/com/qaai/review/*`
- `src/main/java/com/qaai/runner/ScenarioRunnerCommand.java`
- `src/main/java/com/qaai/config/QaaiProperties.java`
- `src/main/java/com/qaai/artifacts/*`
- `src/main/resources/application.yml`
- `README.md`
- `docs/artifacts_model.md`
- `docs/result_contracts.md`
- `docs/architecture.md`
- `docs/project_specs.md`
- `docs/operator-guide.md`
- `docs/phase-18-closeout.md`
- `docs/silver-readiness.md`

## Tests

Narrow tests should cover:

- local provider returns one result per configured lens
- review service rejects missing `transcript.json`
- evidence validation rejects nonexistent turns
- evidence validation rejects mismatched transcript text
- insufficient-evidence lens results are accepted without fabricated findings
- CLI command writes JSON and Markdown artifacts under `outputs/{call_id}/`
- metadata and manifest include multi-lens review paths after success
- disabled provider fails clearly if included in configuration

Expected narrow commands:

```powershell
.\gradlew test --tests "*Review*"
.\gradlew test --tests "*ScenarioRunnerCommandTest" --tests "*QaaiPropertiesTest"
```

Run the full suite if shared artifact metadata or command routing changes are
broad.

## Assumptions

- Phase 18 reviews one existing call at a time.
- A normalized `transcript.json` is required.
- Existing analysis and evaluation artifacts are optional context, not required
  inputs.
- The first implementation uses a deterministic local provider.
- AI-backed review can be added later behind the same provider boundary.

## Risks

- Lens output could duplicate existing analysis or evaluation unless each lens
  has a narrow responsibility.
- A multi-lens layer could be mistaken for pass/fail ownership unless every
  artifact and prompt preserves the advisory boundary.
- Evidence validation needs to stay strict so review output cannot cite
  fabricated transcript text.
- Adding too many lenses at once could weaken test clarity.

## Scope Boundaries

In scope:

- fixed lens registry
- deterministic local review provider
- review JSON and Markdown artifacts
- evidence validation
- metadata and manifest linking
- docs and tests

Out of scope:

- autonomous agent fleets
- dynamic agent-to-agent coordination
- AI-owned workflow control
- AI-owned pass/fail decisions
- automatic scenario promotion
- automatic live-call execution
- mutation of source artifacts
- real patient data

## Project Silver Note

Project Silver extraction rules are secondary to this project. They suggest that
Phase 18 should be shaped as a coherent, testable behavior change with
deterministic fail-to-pass coverage, but they do not define the product
direction.
