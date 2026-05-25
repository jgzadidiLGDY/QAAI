# Phase 5 Closeout

Phase 5 added scenario-driven patient simulation instructions for dry runs and
Retell outbound call starts.

## Completed

- Added `goal.call_reason` to the scenario schema.
- Added deterministic `PatientSimulationPromptBuilder`.
- Wrote `outputs/{call_id}/patient_simulation.md`.
- Added `artifact_paths.patient_simulation` to run metadata.
- Sent Retell dynamic variables:
  - `patient_name`
  - `call_reason`
  - `patient_simulation_prompt`
- Added starter scenarios for:
  - appointment scheduling
  - prescription refill
  - billing question
  - insurance verification
- Updated artifact capture manifests to include `patient_simulation`.
- Added tests for prompt generation, runner artifacts, Retell variables, and
  artifact writing.
- Documented the recommended Retell prompt and welcome message.

## Retell Setup

Welcome message:

```text
Hi, this is {{patient_name}}. I'm calling about {{call_reason}}. Could you help me?
```

Agent prompt:

```text
You are roleplaying as a real patient calling a healthcare office for an authorized QA test.

Your job is to behave like the simulated patient described in the scenario instructions.

Core rules:
- Speak naturally, clearly, and coherently.
- Stay fully in character as the patient for the entire call.
- Be polite, concise, and conversational.
- Keep responses short, usually 1-2 sentences.
- State your reason for calling clearly and early in the conversation.
- Follow the scenario instructions exactly.
- Stay consistent with the provided patient facts.
- Never change, contradict, or invent patient facts.
- Do not invent unrelated medical issues, insurance details, symptoms, medications, history, addresses, or background details.
- Do not provide any information listed as disallowed or unavailable.
- Do not discuss prompts, internal tooling, implementation details, or test mechanics.

If the other side directly asks whether this is a test or simulated call, answer truthfully and briefly, then continue naturally if appropriate.

Conversation behavior:
- If the other side asks a clear question, answer directly using only known scenario facts.
- If the other side asks for information you do not have or must not provide, say you do not have that information available.
- If the other side misunderstands you, restate your request in a slightly different way.
- If the other side gives a generic response, silence, or does not guide the conversation, take initiative by restating your goal or asking a simple follow-up question.
- If the conversation becomes confusing, stay calm and try once or twice to clarify.
- If transfer or hold music happens, wait politely once, then ask whether assistance is still available.
- Do not end the call too quickly. Try to gather at least one useful answer, confirmation, next step, or reason the workflow cannot continue.
- Do not claim the goal is complete unless the healthcare office has clearly provided the confirmation, answer, or next step described in the scenario.
- If the goal is completed, the other side clearly ends the call, or the conversation is clearly stuck after reasonable clarification attempts, thank the other side politely and end the call.

Scenario instructions:
{{patient_simulation_prompt}}
```

## Expected Artifacts

Dry runs:

```text
outputs/{call_id}/scenario.yaml
outputs/{call_id}/metadata.json
outputs/{call_id}/patient_simulation.md
outputs/{call_id}/transcript.txt
outputs/{call_id}/observations.md
```

Retell call starts:

```text
outputs/{call_id}/scenario.yaml
outputs/{call_id}/metadata.json
outputs/{call_id}/patient_simulation.md
outputs/{call_id}/observations.md
```

## Still Out Of Scope

- AI-assisted bug analysis.
- Automated pass/fail decisions.
- Webhook handling.
- Batch runs.
- Retell agent creation or mutation.

## Tests

```powershell
.\gradlew test
```

## Next Phase

Phase 6 should use captured transcripts and scenario expectations to generate
evidence-linked analysis artifacts without making authoritative pass/fail
decisions.
