# Silver Readiness Notes

This repository is structured so hardening changes can become Project Silver
tasks when they have a clear behavioral bug, deterministic tests, and a small
reference solution.

## Silver-Worthy Commit Shape

Each Silver-worthy commit should be usable as a standalone task candidate:

- Base commit: the commit immediately before the fix
- Solution patch: the single coherent fix commit
- Tests: behavior that fails at the base commit and passes after the fix
- Scope: one coherent bug, hardening change, or feature slice per commit

Avoid bundling unrelated cleanup with behavioral commits. Those changes make the
Silver task boundary harder to explain and verify.

## Phase 6 Commit Record

| Candidate | Base commit | Fix commit | Possible instruction | Suggested fail-to-pass behavior |
| --- | --- | --- | --- | --- |
| Evidence-linked call analysis | `e9c38bf` | `041c2be` | Captured calls should be analyzable by local `call_id`, producing advisory JSON and Markdown reports only when findings cite transcript evidence and human review remains required. | A captured run with `transcript.json` produces `analysis.json`, `analysis.md`, updated metadata paths, and manifest entries; reports with missing or unsupported evidence are rejected. |
| CLI call id validation | `6c258e4` | `8ae0777` | CLI commands that load artifacts by `call_id` should reject invalid directory-key characters with a clear error instead of leaking platform path exceptions or stack traces. | Running analysis with a literal placeholder-style call id such as `<retell_call_id_without_transcript>` reports a controlled validation error and exits nonzero; artifact reads do not attempt to resolve the invalid path. |

Candidate invariant:

- Invariant: AI-assisted findings must be grounded in exact normalized transcript evidence and must require human review.
- Symptom: without this phase, captured call artifacts cannot produce structured analysis artifacts for review.
- Root cause: the workflow ended at artifact capture and had no analysis command, report contract, or evidence validation.
- Why it may be Silver-relevant: the task requires connecting command routing, artifact persistence, scenario/transcript loading, AI response validation, and deterministic tests.

Candidate invariant:

- Invariant: user-provided `call_id` values must be validated before artifact paths are resolved.
- Symptom: placeholder-style input such as `<retell_call_id_without_transcript>` can surface as a raw Windows `InvalidPathException`.
- Root cause: artifact loading resolved the `call_id` as a filesystem path segment before checking the allowed call id shape.
- Why it may be Silver-relevant: the task is a small but real CLI hardening fix that crosses command handling, artifact path construction, process exit behavior, and a negative-path regression test.

## Phase 7 Commit Record

| Candidate | Base commit | Fix commit | Possible instruction | Suggested fail-to-pass behavior |
| --- | --- | --- | --- | --- |
| Reproducible run index and artifact completeness | `4b0dc98` | `bdd37b9` | Successful artifact writes should append a local run index entry that records run identity, status, artifact paths, completeness, and warnings without using the index to control workflow decisions. | Dry-run, Retell-start, capture, and analysis artifact writes append JSONL entries; completeness reports required missing artifacts by lifecycle status while unavailable audio is only a warning; `--list-runs` handles empty and populated indexes. |

Candidate invariant:

- Invariant: run history must be reproducible from written artifacts and must not become hidden workflow control state.
- Symptom: before this phase, a reviewer had to inspect individual `outputs/{call_id}` folders manually and had no local lifecycle index or completeness summary.
- Root cause: artifact bundles were persisted per call, but there was no append-only cross-run summary derived from metadata and files on disk.
- Why it may be Silver-relevant: the task requires connecting artifact persistence, metadata contracts, lifecycle statuses, CLI inspection, and deterministic filesystem tests without introducing AI-driven pass/fail behavior.

## Phase 7a Commit Record

| Candidate | Base commit | Fix commit | Possible instruction | Suggested fail-to-pass behavior |
| --- | --- | --- | --- | --- |
| Receptionist transcript role clarity | `46233f8` | `63104e7` | Normalized Retell transcripts should label the simulated patient as `patient` and the target healthcare front desk side as `receptionist`, and analysis evidence should use the same speaker vocabulary. | Captured Retell `user` turns render as `[receptionist]` in `transcript.txt`, `transcript.json` stores `speaker = receptionist`, and analysis prompt/evidence validation accepts `receptionist` evidence. |

Candidate invariant:

- Invariant: normalized speaker labels must describe the QA workflow roles rather than Retell API role names.
- Symptom: target-side transcript turns were labeled `agent`, which was ambiguous across Retell agent, target voice agent, QA agent, and AI agent meanings.
- Root cause: Retell `user` transcript turns were mapped to the generic label `agent` during normalization.
- Why it may be Silver-relevant: the task is small and behavioral, but it crosses transcript capture, human-readable artifacts, analysis prompt contracts, evidence validation fixtures, and documentation.

## Phase 8 Commit Record

| Candidate | Base commit | Fix commit | Possible instruction | Suggested fail-to-pass behavior |
| --- | --- | --- | --- | --- |
| Transcript-aware conversation-quality review | `000640b` | `722419b` | Existing runs should be reviewable by local `call_id`, refreshing `observations.md` from scenario guidance and captured transcript evidence while explicitly marking missing evidence unavailable and preserving human-owned pass/fail decisions. | A run with `metadata.json`, `scenario.yaml`, and `transcript.json` writes observations citing patient and receptionist turn numbers for welcome, initiative, pacing, clarification, and workflow movement; a run without `transcript.json` records evidence as unavailable instead of inventing observations; `--review-conversation` rejects missing `call_id`. |

Candidate invariant:

- Invariant: conversation-quality observations must be grounded in scenario guidance and available transcript turns, and must remain advisory.
- Symptom: before this phase, reviewers had scenario-owned guidance and captured transcripts, but no deterministic command to refresh observations from the actual run evidence.
- Root cause: observations were created at dry-run or call-start time and were not connected to normalized captured transcript turns after Phase 7a role clarification.
- Why it may be Silver-relevant: the task crosses CLI routing, artifact loading and writing, scenario validation, normalized transcript role semantics, prompt behavior, and deterministic filesystem tests without relying on external services.

## Phase 9 Commit Record

| Candidate | Base commit | Fix commit | Possible instruction | Suggested fail-to-pass behavior |
| --- | --- | --- | --- | --- |
| Provider reliability and observability hardening | `9ffdc24` | `5e02594` | External Retell, recording-download, and OpenAI analysis calls should use bounded timeouts, classify provider failures with operation-specific errors, reject malformed provider responses clearly, and emit lifecycle logs without logging secrets, full prompts, or transcript bodies. | Retell and OpenAI client tests cover HTTP failures, missing provider bodies, recording-download failures, configured timeout binding, and command failure context; provider errors include provider/operation/status while analysis errors do not echo the prompt. |
| Reliability follow-up coverage and env example | `58e39e5` | `3b98f4f` | The sample environment file should expose every optional timeout override introduced by reliability hardening, and Retell get-call HTTP failures should be verified with the same provider/operation error contract as other provider calls. | `.env.example` includes Retell API, Retell recording-download, and OpenAI analysis timeout variables; a mocked Retell get-call HTTP 500 fails with provider, operation, status, and response context. |

Candidate invariant:

- Invariant: external provider calls must fail predictably with provider and operation context, and local logs must describe lifecycle progress without becoming another artifact store for sensitive content.
- Symptom: before this phase, provider clients had no explicit timeout configuration, some provider failures used generic error text, OpenAI HTTP errors were not classified before response parsing, and workflow logs did not consistently expose lifecycle progress.
- Root cause: Retell, recording download, and OpenAI integrations were implemented as narrow happy-path clients before repeated real-call operation was the priority.
- Why it may be Silver-relevant: the task crosses configuration binding, HTTP client setup, provider error handling, command failure context, and deterministic client tests while preserving artifact and human-review boundaries.

## Phase 10 Commit Record

| Candidate | Base commit | Fix commit | Possible instruction | Suggested fail-to-pass behavior |
| --- | --- | --- | --- | --- |
| Analyzer provider selection | `3444632` | `580438f` | Call analysis should be selectable between OpenAI, a deterministic local analyzer, and a disabled mode while preserving evidence validation, human-review requirements, and analyzer provider/model metadata. | Configured local analysis produces stable advisory analysis artifacts without network access and records provider/model metadata; disabled analysis fails clearly; OpenAI remains selectable and keeps provider/model reporting. |

Candidate invariant:

- Invariant: analysis provider choice must be explicit and reproducible, while report validation remains provider-independent.
- Symptom: before this phase, `--analyze-call` was coupled to OpenAI, so offline demos and tests could not exercise the full analysis artifact flow without external provider credentials.
- Root cause: the analysis client interface accepted only a prompt string and exposed no provider identity or deterministic local implementation.
- Why it may be Silver-relevant: the task crosses configuration binding, Spring provider selection, artifact metadata, analysis service validation, and deterministic tests without letting AI own pass/fail decisions.

## Phase 11 Commit Record

| Candidate | Base commit | Fix commit | Possible instruction | Suggested fail-to-pass behavior |
| --- | --- | --- | --- | --- |
| Local run inspection UX | `79e2a0d` | `38cdd1a` | Local QA runs should be inspectable by `call_id`, and the run index should be filterable by scenario, status, and run mode without turning the index into workflow control state. | A run with `metadata.json` can be summarized with completeness, artifact paths, warnings, and next-step hints; `--list-runs` filters deterministic index entries; running with no command prints supported workflow help. |

Candidate invariant:

- Invariant: local inspection commands must summarize existing artifacts without creating new workflow state or owning pass/fail decisions.
- Symptom: before this phase, operators could list the raw run index but could not inspect one run, filter noisy run history, or discover the command workflow from the CLI.
- Root cause: run history existed as append-only JSONL entries, but command routing had only a broad list view and no read-only inspection service over metadata and completeness checks.
- Why it may be Silver-relevant: the task crosses CLI routing, artifact metadata loading, index filtering, completeness checks, and deterministic filesystem tests while preserving the artifact and human-review boundaries.

## Phase 12 Commit Record

| Candidate | Base commit | Fix commit | Possible instruction | Suggested fail-to-pass behavior |
| --- | --- | --- | --- | --- |
| Reproducibility metadata | `6cda895` | `52d0353` | Run metadata should record reproducibility context for the command that last produced or updated the artifact bundle, including command name, app version, and optional Git commit, without breaking older metadata that lacks these fields. | Dry-run, artifact capture, and analysis flows write `metadata.json` with `reproducibility.command` and `reproducibility.app_version`; analysis still records provider/model metadata; older metadata fixtures without reproducibility fields still deserialize. |
| MVP+ operator docs | `a5f55f0` | `ef1b249` | Weak/docs-only candidate: the local QA workflow documentation should explain setup, command order, status lifecycle, artifact completeness, troubleshooting, and privacy boundaries for authorized test calls. | Documentation-only change; useful for reviewers and operators, but not a strong Silver task because it has no runtime fail-to-pass behavior. |

Candidate invariant:

- Invariant: artifact bundles should explain which local command produced the current metadata state without making reproducibility fields required workflow inputs.
- Symptom: before this phase, metadata recorded run identity, status, artifact paths, and analysis provider details, but not the command/app context that produced the state under review.
- Root cause: artifact metadata evolved around run artifacts first, before repeated MVP+ operation made provenance and review trust more important.
- Why it may be Silver-relevant: the task crosses metadata schema compatibility, dry-run creation, Retell capture updates, analysis updates, optional local Git context, and deterministic artifact tests.

## Test Expectations

Silver-oriented tests should:

- execute runtime behavior instead of checking source text
- avoid network access
- avoid clock-dependent assertions
- use stable test names that can be listed exactly in task metadata
- keep fail-to-pass coverage aligned with the user-facing task instruction

## Packaging Checks

For repository upload:

- Keep `.git/` in the upload archive
- Do not add `.git/` to `.dockerignore`
- Keep Dockerfile guidance aligned with Project Silver rules
- Keep dependency pinning or constraints guidance visible
