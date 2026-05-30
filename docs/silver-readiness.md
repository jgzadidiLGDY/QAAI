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
| Reproducibility metadata hardening | `e635552` | `972f02f` | Runtime metadata should honor app versions loaded through the project `.env` path, Retell call-start metadata should record provenance like other run states, and older metadata without reproducibility fields should remain readable. | `RuntimeReproducibilityMetadata` uses a system-property app version loaded by `.env`; Retell call-start metadata includes `reproducibility.command = retell-call-start`; legacy metadata without `reproducibility` deserializes with a null optional field. |
| Depth and scenario expansion docs | `24a92f2` | `400ee88` | Weak/docs-only candidate: project planning docs should identify short-call depth review and scenario coverage expansion as the next small phases while preserving human-owned judgment. | Documentation-only change; useful for phase planning, but not a strong Silver task because it has no runtime fail-to-pass behavior. |

Candidate invariant:

- Invariant: artifact bundles should explain which local command produced the current metadata state without making reproducibility fields required workflow inputs.
- Symptom: before this phase, metadata recorded run identity, status, artifact paths, and analysis provider details, but not the command/app context that produced the state under review.
- Root cause: artifact metadata evolved around run artifacts first, before repeated MVP+ operation made provenance and review trust more important.
- Why it may be Silver-relevant: the task crosses metadata schema compatibility, dry-run creation, Retell capture updates, analysis updates, optional local Git context, and deterministic artifact tests.

## Phase 13 Commit Record

| Candidate | Base commit | Fix commit | Possible instruction | Suggested fail-to-pass behavior |
| --- | --- | --- | --- | --- |
| Conversation-depth and short-call review signals | `379cdd2` | `fcd2b7a` | Conversation review should surface advisory depth signals from captured metadata and transcript evidence, distinguishing unknown duration from short, typical, and long calls without making pass/fail decisions. | Captured Retell duration is persisted as seconds; review observations include unknown and typical duration cases, turn-count depth concerns, goal-statement evidence, workflow-question evidence, and confirmation or next-step evidence. |
| Phase 13 depth-review documentation | `fcd2b7a` | `9795ac9` | Weak/docs-only candidate: project docs should explain conversation-depth signals, live calibration, duration interpretation, and the Phase 13 scope boundary. | Documentation-only change; useful for reviewers and operators, but not a strong Silver task because it has no runtime fail-to-pass behavior. |
| Next-step evidence phrase matching | `9815b05` | `9746d5a` | Conversation-depth review should not treat words that merely contain a next-step keyword as confirmation or next-step evidence. | A transcript containing "already" but no confirmation or next-step phrase reports "Confirmation or next step reached: not observed" while preserving short-call duration review. |
| Capture-to-review next-step hint | `ce93eb8` | `162b14d` | After artifact capture succeeds, the CLI should make the next review step discoverable so operators know to refresh conversation-depth observations. | `--capture-artifacts` output includes `next_step: --review-conversation --call-id=<local_call_id>` after reporting captured artifact paths. |
| Final Phase 13 closeout update | `e151f2a` | `8afa5bc` | Weak/docs-only candidate: the phase closeout should mention the final heuristic and CLI hint hardening. | Documentation-only change; useful for phase handoff, but not a strong Silver task because it has no runtime fail-to-pass behavior. |

Candidate invariant:

- Invariant: conversation-depth concerns must be grounded in captured duration or transcript evidence and remain advisory.
- Symptom: before this phase, a captured call could have valid artifacts and analysis inputs while still being too short or shallow for meaningful QA, with no deterministic signal calling that out.
- Root cause: conversation-quality review cited transcript turns for pacing and workflow movement, but did not persist provider duration or summarize depth milestones.
- Why it may be Silver-relevant: the task crosses Retell artifact capture, metadata schema compatibility, transcript review, scenario goal interpretation, deterministic heuristic output, and focused tests without adding AI-owned pass/fail behavior.

## Phase 14 Commit Record

| Candidate | Base commit | Fix commit | Possible instruction | Suggested fail-to-pass behavior |
| --- | --- | --- | --- | --- |
| Scenario coverage metadata | `215a7f6` | `7ed0ce9` | Scenario files should declare explicit coverage metadata describing workflow area, edge-case tags, and review risk focus, and invalid or missing coverage should fail deterministic scenario validation. | Existing scenario YAML files load with required coverage metadata; validation rejects missing coverage, blank coverage fields, and unsupported edge-case tags; scenario discovery validates every YAML file in `scenarios/`. |
| Curated edge-case scenarios | `39c6d0a` | `4a704d9` | The scenario library should include a small curated set of synthetic edge-case workflows for missing facts, unavailable information, transfer or hold recovery, ambiguous next steps, and workflow mismatch. | Dynamic scenario discovery loads and validates the new scenario YAML files, each with coverage metadata, allowed facts, disallowed behavior, conversation-quality guidance, and deterministic patient turns. |
| Scenario coverage documentation | `b1396e3` | `b6c5bce` | Weak/docs-only candidate: project docs should explain the scenario coverage taxonomy, allowed edge-case tags, scenario-to-risk map, and human-review boundary. | Documentation-only change; useful for operators and reviewers, but not a strong Silver task because it has no runtime fail-to-pass behavior. |
| Phase 14 closeout notes | `b9697d3` | `fed5799` | Weak/docs-only candidate: phase closeout should summarize scenario coverage behavior, live calibration, tests, and Silver candidate boundaries. | Documentation-only change; useful for phase handoff, but not a strong Silver task because it has no runtime fail-to-pass behavior. |

Candidate invariant:

- Invariant: every runnable scenario should explain the workflow area and edge-case risk it is intended to exercise.
- Symptom: before this phase, scenarios carried conversation-quality risks but no structured coverage metadata, making scenario growth hard to audit.
- Root cause: the scenario schema was designed around patient behavior before curated coverage planning became the priority.
- Why it may be Silver-relevant: the task crosses YAML fixtures, schema parsing, validation rules, scenario discovery tests, and downstream test fixtures while preserving deterministic local execution.

Candidate invariant:

- Invariant: scenario expansion should add reviewable coverage for explicit workflow risks rather than unstructured scenario volume.
- Symptom: before this phase, the scenario library covered core happy paths and a few unavailable-detail cases, but did not exercise transfer or hold recovery, workflow mismatch, or ambiguous next-step behavior.
- Root cause: earlier phases prioritized end-to-end artifact flow before breadth and edge-case depth.
- Why it may be Silver-relevant: the task is mostly fixture-driven, so it is weaker than runtime logic changes, but dynamic scenario validation gives it deterministic fail-to-pass behavior when required scenario metadata or fields are absent.

## Phase 15 Commit Record

| Candidate | Base commit | Fix commit | Possible instruction | Suggested fail-to-pass behavior |
| --- | --- | --- | --- | --- |
| Deterministic evaluation model | `9ac8061` | `8b243d5` | Captured-call evaluation should expose advisory rubric dimensions for safety, accuracy, empathy, policy, and workflow completion, and the local evaluator should produce deterministic human-reviewed results without guessing when transcript evidence is insufficient. | Local evaluation over a transcript with receptionist evidence produces five scored advisory dimensions with cited transcript evidence; local evaluation without receptionist evidence records insufficient evidence and no score; rubric prompts require human review and reject authoritative pass/fail framing. |
| Evaluate-call artifact workflow | `ef642b5` | `b2f5048` | Captured calls should be evaluable by local `call_id`, producing advisory evaluation JSON and Markdown artifacts only when scored dimensions cite transcript evidence or explicitly mark insufficient evidence. | `--evaluate-call` requires `transcript.json`, writes `evaluation.json` and `evaluation.md`, updates metadata with evaluation paths/provider/model and reproducibility, appends manifest/index entries, rejects fabricated evidence quotes, and preserves human review. |
| Evaluation config binding | `b2f5048` | `3b747fc` | Evaluation provider configuration should bind through the canonical application properties record so the Spring context loads while tests and callers pass the explicit evaluator slot. | Spring context and configuration binding tests load with `qaai.evaluation.provider`; runner tests construct `QaaiProperties` with the explicit evaluation argument instead of relying on a noncanonical compatibility constructor. |

Candidate invariant:

- Invariant: rubric-specific evaluation output must stay advisory, dimension-specific, and grounded in available transcript evidence.
- Symptom: before this phase, the project had AI-assisted bug analysis but no separate evaluation contract for consistent safety, accuracy, empathy, policy, and workflow-completion review signals.
- Root cause: the workflow ended at analysis and conversation-quality observations, before a distinct evaluation layer existed.
- Why it may be Silver-relevant: the task crosses result contracts, provider boundaries, deterministic local behavior, rubric prompt construction, and evidence-insufficiency handling without relying on external services.

Candidate invariant:

- Invariant: evaluation artifacts must be produced only from captured transcript evidence and must update run metadata without becoming pass/fail workflow control.
- Symptom: before this phase, a captured call could be analyzed for suspected bugs, but there was no command that wrote rubric-specific evaluation artifacts linked to transcript evidence.
- Root cause: artifact writing, metadata completeness, manifest updates, CLI routing, and run inspection did not yet know about evaluation outputs.
- Why it may be Silver-relevant: the task requires tracing a local command through artifact loading, scenario loading, transcript validation, provider output validation, metadata updates, manifest/index writes, and CLI inspection tests.

Candidate invariant:

- Invariant: adding evaluator configuration must preserve Spring configuration-property binding and explicit test construction.
- Symptom: an overloaded compatibility constructor let unit tests compile but caused the Spring context to fail with no default constructor during configuration binding.
- Root cause: record configuration properties should expose the canonical constructor shape expected by Spring instead of adding a shortcut constructor with a different argument list.
- Why it may be Silver-relevant: this is a small hardening candidate with deterministic context-load and configuration-binding tests, but it is narrower than the artifact workflow candidate.

## Phase 16 Commit Record

| Candidate | Base commit | Fix commit | Possible instruction | Suggested fail-to-pass behavior |
| --- | --- | --- | --- | --- |
| Static QA report generation | `ad0955d` | `904c3c6` | Local QA runs should be summarizable through a generated static report that reads existing run, analysis, evaluation, and scenario coverage artifacts without mutating run state or creating pass/fail decisions. | `--generate-report` writes `report.json`, `report.md`, and `index.html` under `outputs/reports/{report_id}`; the report deduplicates latest runs from the run index, summarizes evaluation scores and insufficient-evidence counts, counts analysis severities, includes scenario coverage metadata, and links back to raw artifacts. |
| Phase 16 report workflow docs | `8d3b619` | `5ac8ab3` | Weak/docs-only candidate: project docs should explain static report generation, report artifacts, operator usage, contracts, architecture, and Phase 16 closeout boundaries. | Documentation-only change; useful for operators and reviewers, but not a strong Silver task because it has no runtime fail-to-pass behavior. |

Candidate invariant:

- Invariant: report generation must summarize existing trusted artifacts without becoming a source of truth for run status or reviewer judgment.
- Symptom: before this phase, operators could inspect single runs and list the run index, but there was no local artifact-level view across run history, evaluations, analysis findings, and scenario coverage.
- Root cause: Phase 15 produced durable evaluation outputs, but no deterministic aggregation layer existed to make those outputs reviewable across a local corpus.
- Why it may be Silver-relevant: the task crosses CLI routing, index reading, metadata loading, analysis/evaluation parsing, scenario coverage parsing, static artifact writing, and deterministic filesystem tests while preserving human-owned pass/fail decisions.

## Phase 17 Commit Record

| Candidate | Base commit | Fix commit | Possible instruction | Suggested fail-to-pass behavior |
| --- | --- | --- | --- | --- |
| Scenario generation planning docs | `d6f138b` | `19eb196` | Weak/docs-only candidate: project docs should describe AI-assisted scenario draft generation, review-artifact boundaries, expected outputs, contracts, and human promotion requirements. | Documentation-only change; useful for phase planning, but not a strong Silver task because it has no runtime fail-to-pass behavior. |
| Scenario draft generation workflow | `49f660f` | `b0e49f8` | Given an agent-under-test description, the CLI should generate review-only scenario draft artifacts using the configured scenario-generation provider, validate each draft deterministically, and avoid promoting generated YAML into the canonical `scenarios/` library. | `--generate-scenarios --agent-description=<description>` writes agent description, coverage plan, draft YAML files, and JSON/Markdown generation reports under `outputs/scenario-generation/{generation_id}`; reports include provider/model, human review requirement, per-draft validation results, coverage summaries, and warnings; no files are written to `scenarios/`; OpenAI provider errors are classified without leaking the prompt. |
| Phase 17 scenario generation docs | `75c70ec` | `b79a25e` | Weak/docs-only candidate: project docs should explain scenario generation usage, OpenAI provider configuration, output artifacts, closeout boundaries, and human promotion requirements. | Documentation-only change; useful for operators and reviewers, but not a strong Silver task because it has no runtime fail-to-pass behavior. |
| Disabled scenario generation CLI coverage | `d664f9e` | `c551ecb` | Weak/test-only candidate: scenario generation CLI coverage should verify that a disabled provider fails clearly through command routing. | `--generate-scenarios --agent-description=medical-office-scheduling-agent` exits nonzero and reports "Scenario generation is disabled" when the scenario generation provider rejects the request. |
| Scenario generation command examples | `1627fe3` | `837b358` | Weak/docs-only candidate: PowerShell examples for scenario generation should avoid fragile nested quotes so Gradle does not interpret words in the agent description as task names. | Documentation-only change; command examples use `--args='--generate-scenarios --agent-description=medical-office-scheduling-agent'` instead of nested quotes with spaces. |

Candidate invariant:

- Invariant: AI-generated scenarios must remain review artifacts until a human explicitly promotes them.
- Symptom: before this phase, the project had scenario validation and coverage metadata, but no command for drafting a broader scenario set from an agent-under-test description.
- Root cause: scenario coverage was curated manually, and the workflow had no provider boundary, generation report, or validation artifact for AI-assisted scenario drafting.
- Why it may be Silver-relevant: the task crosses CLI routing, provider configuration, OpenAI response parsing, YAML artifact writing, deterministic scenario validation, report generation, and tests that prove generated drafts do not enter `scenarios/` automatically.

## Phase 18 Planning Record

Phase 18 is proposed as structured multi-lens review over existing call
artifacts. Project Silver extraction rules are secondary here: they help keep
the eventual implementation task-shaped, but they do not define the product
direction.

Potential candidate:

| Candidate | Base commit | Fix commit | Possible instruction | Suggested fail-to-pass behavior |
| --- | --- | --- | --- | --- |
| Structured multi-lens review planning docs | `ddf2755` | `fd7fe6b` | Weak/docs-only candidate: project docs should describe structured multi-lens review scope, artifacts, contract direction, risks, and the boundary against autonomous multi-agent orchestration. | Documentation-only change; useful for phase planning, but not a strong Silver task because it has no runtime fail-to-pass behavior. |
| Structured multi-lens review workflow | `5a2447d` | `85d0b37` | Captured calls should be reviewable through several stable advisory lenses over the same transcript evidence, producing JSON and Markdown artifacts without creating pass/fail decisions or autonomous orchestration. | `--multi-lens-review --call-id=<local_call_id>` requires `transcript.json`, writes `multi-lens-review.json` and `multi-lens-review.md`, includes safety, consistency, patient realism, adversarial robustness, and workflow risk lens results, validates transcript evidence for concrete findings, accepts explicit insufficient-evidence results, and links artifacts through metadata/manifest entries. |
| Disabled multi-lens review CLI coverage | `696a622` | `8759f18` | Weak/test-only candidate: multi-lens review command coverage should verify that a disabled provider fails clearly through command routing. | `--multi-lens-review --call-id=<local_call_id>` exits nonzero and reports "Multi-lens review is disabled" when the review service rejects the request. |

Candidate invariant:

- Invariant: specialized review lenses must inspect fixed existing artifacts and
  remain advisory.
- Symptom: after Phase 17, the project can generate scenarios, capture calls,
  analyze findings, evaluate dimensions, and generate reports, but it does not
  yet provide a bounded way to compare several specialized review perspectives
  over the same call.
- Root cause: existing analysis and evaluation flows are useful but not
  organized as a stable registry of specialized review lenses.
- Why it may be Silver-relevant: the task can cross CLI routing, provider
  configuration, artifact loading, evidence validation, metadata/manifest
  updates, Markdown rendering, and deterministic tests while preserving
  human-owned review decisions.

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

## Phase 19 Planning Record

Phase 19 is proposed as channel-neutral scenario and interaction modeling.
Project Silver extraction rules remain secondary: they can help identify clean
future task boundaries, but they do not define this project's product direction
or implementation scope.

Potential candidate:

| Candidate | Base commit | Fix commit | Possible instruction | Suggested fail-to-pass behavior |
| --- | --- | --- | --- | --- |
| Channel-neutral scenario model planning docs | `3543550` | `a78500d` | Weak/docs-only candidate: project docs should clarify that voice is the first channel adapter while the reusable QA framework should support future text, email, and web-agent channels through explicit channel boundaries. | Documentation-only change; useful for phase planning, but not a strong Silver task because it has no runtime fail-to-pass behavior. |

Likely stronger candidates should wait until Phase 19 or Phase 20 introduces
testable runtime behavior, such as channel metadata propagation, a shared
interaction abstraction, or a text chat runner that reuses the existing artifact
and review pipeline.

| Candidate | Base commit | Fix commit | Possible instruction | Suggested fail-to-pass behavior |
| --- | --- | --- | --- | --- |
| Channel metadata propagation | `316ce7a` | `fa256db` | Runs should record an explicit interaction channel while preserving existing voice commands, local ids, Retell ids, artifact paths, and older metadata compatibility. | Dry-run and Retell metadata include `channel = voice`; older `dry_run` and `retell` metadata without `channel` deserialize as voice; Retell request metadata includes the channel; run index entries, CLI inspection, and static reports expose the channel. |

Candidate invariant:

- Invariant: execution channel must be explicit metadata while `call_id` remains
  the local artifact id and Retell fields remain voice-channel details.
- Symptom: before this phase, shared run artifacts exposed `run_mode` but had no
  channel field, so future text execution would have to infer channel from
  voice-specific modes or duplicate the artifact pipeline.
- Root cause: the project began as voice-first and encoded Retell/dry-run modes
  before channel-neutral execution became a platform goal.
- Why it may be Silver-relevant: the task crosses metadata serialization,
  backward compatibility, Retell request metadata, run indexing, CLI
  inspection, static reporting, and deterministic tests without adding a second
  runtime channel.

## Phase 20 Commit Record

| Candidate | Base commit | Fix commit | Possible instruction | Suggested fail-to-pass behavior |
| --- | --- | --- | --- | --- |
| Local text chat runner | `57f255a` | `0a38413` | Scenario runs should support a deterministic local text chat mode that writes inspectable channel-specific artifacts while reusing the existing scenario, transcript, artifact, inspection, review, evaluation, analysis, and reporting pipeline. | `--scenario=<path> --run-mode=text-chat` writes `metadata.json` with `run_mode = text_chat` and `channel = text`, writes `transcript.txt` and `transcript.json`, omits Retell/audio/manifest fields, routes through CLI run-mode handling, and reports complete artifacts without voice audio warnings. |
| Phase 20 text chat runner docs | `d30c1c9` | `3c16fd9` | Weak/docs-only candidate: project docs should explain local text chat execution, artifact expectations, lifecycle status, contract boundaries, and closeout notes. | Documentation-only change; useful for operators and reviewers, but not a strong Silver task because it has no runtime fail-to-pass behavior. |
| Text chat reproducibility metadata | `cd27491` | `a91b7b5` | Text chat runs should record reproducibility metadata for the local command that produced the artifact bundle, matching the provenance expectations of other artifact-producing run modes. | A text chat run records `reproducibility.command = text-chat` in metadata, and the focused runner test verifies the command provenance. |
| Run-mode scenario requirement UX | `0bae3bc` | `fd7df5b` | CLI scenario execution should report a clear validation error when `--run-mode` is provided without `--scenario`, while preserving the generic help output for no-argument usage. | `--run-mode=text-chat` exits nonzero with a scenario-path requirement and no Java stack trace; running with no command still prints the supported command help. |

Candidate invariant:

- Invariant: a non-voice interaction channel must produce normalized transcript
  evidence and local artifacts without depending on Retell or voice-specific
  audio/manifest assumptions.
- Symptom: after Phase 19, channel metadata existed, but all runnable paths were
  still voice-oriented, so the channel boundary had not been proven by a second
  execution mode.
- Root cause: the runner and artifact completeness flow were originally built
  around dry-run voice simulation and Retell capture before text interaction was
  introduced.
- Why it may be Silver-relevant: the task crosses CLI routing, scenario
  validation, deterministic transcript generation, metadata serialization,
  artifact writing, channel-aware completeness checks, and focused tests while
  preserving human-owned review decisions.

## Phase 20a Commit Record

| Candidate | Base commit | Fix commit | Possible instruction | Suggested fail-to-pass behavior |
| --- | --- | --- | --- | --- |
| Additive lifecycle metadata preservation | `1b1d432` | `190306e` | Lifecycle commands that update an existing run should preserve previously linked artifact paths and advisory provider metadata while only replacing the artifacts produced by the current command. | Running analysis after evaluation preserves evaluation and multi-lens paths; running evaluation after analysis preserves analysis and multi-lens paths; rerunning Retell artifact capture preserves existing analysis, evaluation, and multi-lens links; multi-lens review preserves existing analysis/evaluation metadata and duration. |

Candidate invariant:

- Invariant: artifact-producing lifecycle commands must update metadata
  additively so existing review evidence remains linked unless the command
  intentionally replaces it.
- Symptom: rerunning commands out of the original happy-path order could drop
  artifact links or provider metadata for evaluation, analysis, or multi-lens
  review outputs.
- Root cause: several services rebuilt `ArtifactPaths` and `RunMetadata` with
  older constructor shapes that did not carry every later-phase metadata field.
- Why it may be Silver-relevant: the task crosses capture, analysis,
  evaluation, multi-lens review, metadata serialization, and deterministic
  rerun-order tests without adding new workflow control or pass/fail ownership.
