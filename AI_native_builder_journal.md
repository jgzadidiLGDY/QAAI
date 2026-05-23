# AI-Native Building Notes

This project is being built through a phased AI-native workflow: small product
increments, explicit phase boundaries, deterministic QA behavior first, and
reviewable closeout notes after each milestone.

The goal of this file is to make that workflow visible from the repository root,
alongside `README.md`. The README explains the project purpose, plan, stack, and
expected structure; these notes explain how the platform is being built.

## Workflow Shape

Each phase is treated as a narrow product milestone:

- define the phase goal before implementation
- keep the implementation small enough to review
- preserve deterministic execution and result contracts
- add or update tests with the behavior change
- update docs in the same phase
- close the phase with a handoff note

This is especially important for an AI QA platform because capability should grow
from structure, not from opaque automation. The build process intentionally
keeps AI assistance grounded in visible inputs, deterministic outputs, and
reviewable evidence.

## Build Principles In Practice

- Deterministic QA logic comes before AI analysis.
- AI assists with analysis, but humans own pass/fail decisions.
- Every result should be inspectable through scenario inputs, transcripts,
  artifacts, evaluations, findings, and reconciliation metadata.
- Live-call lessons are folded back into deterministic tests and scenario rules.
- Operational recovery paths, such as Retell backfill, are documented as recovery
  tools rather than hidden normal behavior.

## Phase Closeout Trail

The phase closeout files will become the primary evidence of the build process:

- [Phase 0 Closeout](docs/phase-0-closeout.md)

## Latest Phase Closeout Notes

Phase 0 created the runnable Java/Spring Boot foundation, configuration contract,
environment template, setup docs, and basic tests. It intentionally left scenario
execution, Retell calls, artifact capture, and OpenAI analysis for later phases.

## Additional Implementation Notes

Additional notes will be made available for future implementation or extensions.

## How To Read This Trail

For a quick product view, start with [README.md](README.md).

For architecture and contracts, these planned docs will be added as the project
reaches the relevant phases:

- [Architecture](docs/architecture.md)
- [Result Contracts](docs/result-contracts.md)
- [Artifact Model](docs/artifact-model.md)

For the AI-native build history, read the phase closeouts in order. They show
what changed, what stayed out of scope, what tests were added, and what follow-up
work became visible from each milestone.

## Why This Matters

This repository is not just the output of an AI-assisted build process. It is
also meant to preserve evidence of that process:

- decisions are made phase by phase
- AI boundaries are documented
- deterministic behavior remains inspectable
- live-call observations become tests and contracts
- future contributors can see why the system evolved the way it did

That history should make the project easier to review, safer to extend, and more
honest about what the platform can and cannot claim at each stage.
