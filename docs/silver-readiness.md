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

Candidate invariant:

- Invariant: AI-assisted findings must be grounded in exact normalized transcript evidence and must require human review.
- Symptom: without this phase, captured call artifacts cannot produce structured analysis artifacts for review.
- Root cause: the workflow ended at artifact capture and had no analysis command, report contract, or evidence validation.
- Why it may be Silver-relevant: the task requires connecting command routing, artifact persistence, scenario/transcript loading, AI response validation, and deterministic tests.

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
