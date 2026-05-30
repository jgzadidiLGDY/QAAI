# Phase 21 Plan

## Summary

Phase 21 should make QAAI feel like a platform for evaluating AI agents, not
only a single medical receptionist workflow. The stage introduces
agent-under-test profiles and deterministic suite runs while preserving the
existing run artifact model.

The medical receptionist remains the first sample agent. The product concept
becomes broader: a named agent profile, a reviewed scenario suite, an explicit
channel, ordinary per-scenario run artifacts, and a suite-level summary for
human review.

## Implementation Plan

1. Add an agent profile YAML format and loader.
2. Add validation for required profile fields, supported workflows, and channel
   declarations.
3. Add a suite YAML format that references one agent profile and an ordered set
   of existing scenario files.
4. Add validation that suite scenarios exist, load successfully, and remain
   deterministic reviewed scenarios.
5. Add a suite execution command that runs scenarios through `text-chat` first.
6. Extend run metadata and run index entries with `agent_profile_id`,
   `suite_id`, and `suite_run_id`.
7. Write suite-level artifacts under `outputs/suites/{suite_run_id}/`.
8. Update operator docs and README usage examples after behavior lands.

## Likely Files

- `agent-profiles/medical-receptionist-demo.yaml`
- `suites/receptionist-smoke.yaml`
- `src/main/java/com/qaai/agent/*`
- `src/main/java/com/qaai/suite/*`
- `src/main/java/com/qaai/runner/ScenarioRunnerCommand.java`
- `src/main/java/com/qaai/artifacts/RunMetadata.java`
- `src/main/java/com/qaai/artifacts/RunIndexEntry.java`
- `src/main/java/com/qaai/reporting/*` if static reports expose profile or
  suite fields
- `docs/operator-guide.md`
- `docs/result_contracts.md`
- `docs/artifacts_model.md`

## Tests

- Agent profile loader accepts a valid profile and rejects missing required
  fields.
- Suite loader accepts a valid suite and rejects unknown profile ids, missing
  scenario files, unsupported run modes, and empty scenario lists.
- Suite command creates one normal run bundle per scenario using `text-chat`.
- Each generated run metadata file records `agent_profile_id`, `suite_id`, and
  `suite_run_id`.
- Suite report JSON and Markdown link to the generated run bundles.
- Existing single-scenario dry-run, text-chat, and Retell command behavior
  remains unchanged.

## Assumptions

- Phase 21 should use `text-chat` as the first suite execution path because it
  is deterministic, local, and does not risk accidental batch phone calls.
- Voice profiles may be declared in YAML, but live Retell suite execution should
  remain out of scope until a later phase explicitly approves safe batching.
- Profiles and suites are committed review inputs, unlike generated outputs
  under `outputs/`.
- `call_id` remains the local run identifier for individual runs.

## Risks

- Suite execution could drift into hidden orchestration if it starts chaining
  analysis, evaluation, multi-lens review, or reporting automatically.
- Agent profiles could accidentally absorb secrets. Profiles should reference
  target settings and provider names, while credentials remain environment
  backed.
- A too-large suite feature could blur into dashboard or database work.
- Batch real-call support could create operational risk if added before
  approvals, rate limits, and operator safeguards are explicit.

## Scope Boundaries

Phase 21 should not include:

- database storage
- web UI or dashboard changes
- automatic pass/fail decisions
- automatic downstream analysis, evaluation, or multi-lens review for every
  suite run
- live batch Retell execution by default
- generated scenario promotion
- broad renaming of `call_id`
- migration of historical artifacts

## Project Silver Notes

Project Silver is secondary context only. It should shape how implementation
work is sliced, not what the product becomes.

For Phase 21, useful Silver-aligned constraints are:

- make each behavioral slice deterministic and testable
- prefer tests that verify behavior rather than source structure
- keep commits coherent enough that a future extraction can identify a base
  commit, a solution patch, and fail-to-pass tests
- avoid vague tasks, nondeterministic tests, external network requirements, and
  hidden state

These constraints support good engineering discipline for QAAI, but the actual
product direction remains the QAAI platform roadmap.
