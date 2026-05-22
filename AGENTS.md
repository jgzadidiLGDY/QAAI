## Goal

Build the Voice AI QA Agent from scratch using Codex as an implementation partner.

This project is NOT a production healthcare voice platform.

The goal is to incrementally build:

- a scenario-driven patient QA bot
- outbound call execution
- transcript + artifact capture
- AI-assisted bug analysis
- reproducible QA workflows

The system should evolve through small, reviewable stages.

Each stage should:

- work end-to-end
- remain understandable
- preserve deterministic execution flow
- produce practical artifacts

---

## Product Direction

The long-term system includes:

- scenario-driven patient behavior
- outbound voice call execution
- transcript capture
- audio recording collection
- artifact storage
- AI-assisted bug identification
- structured reporting

Do NOT attempt to build all functionality immediately.

---

## Core Working Model

Treat every stage as a product milestone.

Each stage should:

- solve one meaningful problem
- preserve runnable behavior
- remain easy to inspect
- finish with tests/docs where appropriate

---

## Stage 2 — Conversation Quality Iteration

Goal:

Improve realism and conversational stability.

Focus:

- prompt refinement
- welcome behavior
- initiative-taking
- conversation pacing

Artifacts:

- before/after observations

---

## Proposal Before Implementation

Before implementing a stage:

Codex should provide:

- summary
- implementation plan
- likely files
- tests
- assumptions
- risks
- scope boundaries

Implementation begins after approval.

---

## Deterministic Workflow First

Prefer:

1. scenario inputs
2. deterministic execution
3. artifact generation
4. persistence
5. AI analysis
6. docs

---


## AI Feature Rules

Allowed:

- transcript summarization
- bug detection
- issue clustering
- report generation

Disallowed:

- core workflow control
- pass/fail ownership
- fabricated evidence

AI assists.

Humans decide.

---

## Artifact Rules

Artifacts include:

- transcript
- metadata
- wav recording
- bug analysis output

All artifacts:

- stored under outputs/
- linked by call_id
- reproducible

---

## Git Rules

- Commit before and after major changes
- Prefer small, coherent commits over large mixed commits
- One commit should represent one understandable change:

  - one bug/fix
  - one feature slice
  - one docs-only update
  - one testable hardening improvement

- Keep unrelated cleanup out of behavioral commits
- When a change is meant to be testable, include the relevant tests in the same commit
- Before committing, run the narrow relevant tests first; run the full suite when the change affects shared behavior
- Check `git status` and `git diff` before staging
- Stage only the files that belong to the intended commit
- After committing, confirm the worktree is clean unless there is an intentional follow-up change
- For future task extraction, shape behavioral commits so a reviewer can identify:

  - base commit: immediately before the fix
  - solution patch: the single fix commit
  - fail-to-pass behavior: tests that fail before and pass after

- Use clear commit messages:

  - "Phase 1: xxx"

---

## Definition of Done (Per Stage)

A stage is complete when:

- feature works locally
- execution flow is understandable
- artifacts are inspectable
- docs remain current
- scope stays controlled

---

## Anti-Patterns

Avoid:
- multi-agent orchestration
- large refactors
- hidden AI behavior

---

## Collaboration Expectations

Codex acts as:

- implementation partner
- reviewer
- debugger

Human remains:

- architect
- product owner
- final reviewer

---

## Final Principle

Improve through:

- real calls
- real artifacts
- reproducible scenarios
- grounded analysis

Optimize for:

small phases → stable behavior → disciplined iteration