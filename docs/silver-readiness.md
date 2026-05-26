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

Candidate invariant:

- Invariant: external provider calls must fail predictably with provider and operation context, and local logs must describe lifecycle progress without becoming another artifact store for sensitive content.
- Symptom: before this phase, provider clients had no explicit timeout configuration, some provider failures used generic error text, OpenAI HTTP errors were not classified before response parsing, and workflow logs did not consistently expose lifecycle progress.
- Root cause: Retell, recording download, and OpenAI integrations were implemented as narrow happy-path clients before repeated real-call operation was the priority.
- Why it may be Silver-relevant: the task crosses configuration binding, HTTP client setup, provider error handling, command failure context, and deterministic client tests while preserving artifact and human-review boundaries.

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
