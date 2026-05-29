# Project Specs

This document describes what the Voice AI QA Agent is expected to become, what the first small version should do, and what remains intentionally out of scope.

## Product Goal

Build a reproducible QA platform for testing healthcare voice agents through real outbound calls and scenario-driven patient behavior.

The platform should help a human reviewer answer:

- Was the target voice agent reachable?
- Did the patient simulation follow the scenario?
- Did the conversation complete the expected workflow?
- What transcript and recording evidence supports any finding?
- Can the same scenario be rerun and compared later?

## Current Target

The initial target healthcare voice agent is:

```text
+18054398008
```

All calls must use authorized test scenarios and avoid real patient data.

## Small Project Scope

The first useful version should:

- load a patient scenario
- execute a deterministic dry run
- place an outbound Retell call in a later phase
- capture transcript, audio, and metadata artifacts
- normalize artifacts under `outputs/{call_id}/`
- run AI-assisted analysis over completed artifacts
- produce structured bug findings with transcript evidence

## MVP+ Scope

The MVP+ project stage starts after the basic end-to-end workflow exists:
scenario execution, Retell call start, artifact capture, AI-assisted analysis,
run indexing, and conversation-quality review.

MVP+ should make that workflow reliable enough for repeated local QA use by a
human reviewer. The system should:

- bound external Retell, OpenAI, and recording-download calls with timeouts
- report provider and command failures with useful local context
- log important lifecycle events without exposing secrets or full transcript content
- keep AI analysis behind a pluggable module boundary
- support a deterministic local analyzer for tests, demos, and offline review
- make runs easy to inspect through focused CLI commands
- record enough metadata to reproduce or compare a run later
- keep README and operator docs accurate for setup, run, capture, review, analyze, and test workflows

MVP+ should not add broad orchestration, autonomous pass/fail decisions, or a
database unless a later milestone proves they are necessary.

## Mini-Scope Expansion After MVP+

After Phase 12, two product risks become important:

- calls may be too short or shallow to produce meaningful workflow evidence
- the initial scenario set may not cover enough workflows or edge cases

The next expansion should address these risks in small reviewable phases.

Phase 13 adds conversation-depth and short-call review signals. These signals
are advisory and evidence-based. They highlight calls with too few transcript
turns, very short or unknown duration, no stated goal, no workflow-specific
question, or no confirmation/next step, but they do not own pass/fail decisions.

Phase 14 should expand scenario coverage with a curated taxonomy. New scenarios
should state the workflow or edge case they exercise, remain synthetic, avoid
real patient data, and stay deterministic enough for validation and repeated
runs.

Phase 14 implements the first version of that taxonomy through reviewer-facing
scenario `coverage` metadata. The current library maps each scenario to a
workflow area, edge-case tags, and a risk focus in
[Scenario Coverage](scenario-coverage.md).

## Evaluation and Reporting Expansion

After Phase 14, the next scope expansion should turn individual call artifacts
into a consistent advisory evaluation layer before building a dashboard.

Phase 15 should add evidence-linked evaluation infrastructure. Evaluations
should be separated by dimension, such as:

- safety
- accuracy
- empathy
- policy
- workflow completion

Each dimension should have its own rubric prompt and structured result. A result
may include a score, rationale, uncertainty, and transcript evidence, but it
must remain advisory and set `human_review_required = true`. Missing evidence
should produce an explicit unavailable or insufficient-evidence result rather
than a guessed score.

Phase 16 visualizes evaluation and artifact data through a local static report.
The report reads existing artifacts rather than creating hidden workflow state.
Useful views include call history, evaluation score summaries, bug severity
distribution, scenario coverage, and links to raw artifacts.

## Scenario Generation Expansion

After evaluation and reporting exist, scenario coverage becomes the next major
input-quality bottleneck. Phase 17 adds AI-assisted scenario draft generation
from an agent-under-test description, such as:

```text
medical office scheduling agent
```

Generated scenarios should be draft artifacts first. The system may use AI to
propose workflow areas, edge cases, patient goals, allowed facts, disallowed
behavior, and conversation-quality risks, but deterministic validation and
human review must decide whether any draft is promoted into `scenarios/`.

The first implementation should write draft outputs under:

```text
outputs/scenario-generation/{generation_id}/
```

The goal is diverse, reviewable coverage across declared workflow areas and
known edge-case categories. The system should not claim full behavioral-space
coverage.

## Structured Multi-Lens Review Expansion

After scenario generation, the next platform step should evolve toward
specialized review without adopting autonomous multi-agent orchestration.
Phase 18 should add a bounded multi-lens review layer over existing call
artifacts.

Each review lens should inspect the same fixed evidence bundle:

- scenario snapshot
- run metadata
- normalized transcript
- existing analysis output when available
- existing evaluation output when available

Initial lenses should cover safety, consistency, patient realism, adversarial
robustness, and workflow risk. Each lens should produce advisory structured
findings with transcript evidence, or explicitly state that evidence is
insufficient. A combined Markdown report should make the results easy for a
human reviewer to inspect.

This is deliberately not a fleet of autonomous agents. The lenses should not
control workflow, call each other dynamically, change run status, promote
scenarios, trigger live calls, or decide pass/fail.

## Non-Goals

This project is not:

- a production healthcare voice platform
- a clinical decision system
- a source of truth for pass/fail decisions
- a replacement for human QA review
- a place to store real patient data

## Users

Primary users:

- QA reviewer
- voice agent builder
- product owner reviewing workflow reliability

Secondary users:

- developer extending the runner
- reviewer auditing artifacts and analysis

## Core Workflows

Initial scenario workflows:

- appointment scheduling
- appointment rescheduling
- prescription refill
- billing question
- insurance verification

Future scenario coverage should add intentionally selected edge cases, such as:

- missing or partial patient facts
- unavailable insurance or demographic information
- unclear names, dates, or callback details
- transfers, hold behavior, silence, or dead air
- ambiguous next steps or incomplete confirmations
- target-side misunderstanding and patient clarification
- wrong department or workflow mismatch

## Success Criteria

A phase is successful when:

- behavior is runnable locally
- execution flow is understandable
- artifacts are inspectable
- docs are current
- tests cover the behavior introduced
- scope remains controlled

An MVP+ milestone is successful when:

- external calls have timeout and error behavior covered by tests
- artifacts remain linked by `call_id` and are inspectable on disk
- analysis remains advisory, evidence-linked, and replaceable
- run inspection helps a reviewer understand current state without reading every file manually
- setup, run, capture, review, analyze, and test commands are documented accurately

The post-MVP+ depth and coverage expansion is successful when:

- short or shallow calls are surfaced as advisory review concerns
- scenario coverage is documented by workflow and edge-case intent
- new scenarios remain deterministic, synthetic, and easy to inspect
- expanded coverage improves evidence quality without adding hidden automation

The evaluation and reporting expansion is successful when:

- rubric-specific evaluations are grounded in cited transcript evidence
- scores remain advisory and require human review
- safety, accuracy, empathy, policy, and workflow completion are evaluated
  independently
- historical imported transcripts can be evaluated without pretending they were
  native QAAI runs
- reporting summarizes existing artifacts without owning pass/fail decisions

The scenario generation expansion is successful when:

- an agent-under-test description produces a bounded draft scenario set
- generated drafts pass deterministic scenario validation
- coverage plans explain intended workflow and edge-case breadth
- generated drafts remain review artifacts until a human promotes them
- no generated draft contains real patient data

The structured multi-lens review expansion is successful when:

- one captured call can be reviewed through several stable, named lenses
- concrete findings cite transcript evidence
- insufficient evidence is represented explicitly instead of guessed
- JSON and Markdown review artifacts are linked by `call_id`
- deterministic local tests can exercise the workflow without network access
- the implementation preserves human-owned review and avoids autonomous
  orchestration

## AI Boundary

AI may assist with:

- transcript summarization
- workflow issue detection
- issue clustering
- report generation
- rubric-specific advisory evaluation
- scenario draft generation
- coverage-plan drafting
- specialized advisory review lenses

AI must not:

- control the core workflow
- fabricate evidence
- own final pass/fail decisions
- replace human review
