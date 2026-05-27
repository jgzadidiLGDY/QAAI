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
- [Phase 1 Closeout](docs/phase-1-closeout.md)
- [Phase 2 Closeout](docs/phase-2-closeout.md)
- [Phase 3 Closeout](docs/phase-3-closeout.md)
- [Phase 4 Closeout](docs/phase-4-closeout.md)
- [Phase 5 Closeout](docs/phase-5-closeout.md)
- [Phase 6 Closeout](docs/phase-6-closeout.md)
- [Phase 7 Closeout](docs/phase-7-closeout.md)
- [Phase 7a Closeout](docs/phase-7a-closeout.md)
- [Phase 8 Closeout](docs/phase-8-closeout.md)
- [Phase 9 Closeout](docs/phase-9-closeout.md)
- [Phase 10 Closeout](docs/phase-10-closeout.md)
- [Phase 11 Closeout](docs/phase-11-closeout.md)

## Latest Phase Closeout Notes

Phase 11 added local run inspection UX. Operators can now inspect one run by
`call_id`, filter the run index by scenario, status, or run mode, and see concise
help when no command is provided while preserving artifact-owned evidence and
human-owned review.

## MVP+ Direction

After the interim review, the project scope expands into an MVP+ stage. MVP+
keeps the same disciplined build model, but shifts the next milestones toward
real-world repeatability:

- Phase 9: reliability and observability hardening
- Phase 10: analyzer pluggability
- Phase 11: run inspection and workflow UX
- Phase 12: MVP+ documentation and artifact trust

This expansion does not change the core boundary: deterministic workflow code
owns orchestration, artifacts provide evidence, AI assists with analysis, and
humans own final review.

## Additional Implementation Notes

Additional notes will be made available for future implementation or extensions.

## How To Read This Trail

For a quick product view, start with [README.md](README.md).

For project scope, architecture, artifacts, and result contracts, read:

- [Project Specs](docs/project_specs.md)
- [Architecture](docs/architecture.md)
- [Artifacts Model](docs/artifacts_model.md)
- [Result Contracts](docs/result_contracts.md)

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
