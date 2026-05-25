# Scenario Format

Scenarios define patient behavior and expected workflow outcomes. They are intended to make call execution reproducible and reviewable.

Phase 1 introduces the first parsed scenario schema and deterministic dry-run behavior.

## Planned YAML Shape

```yaml
id: appointment_reschedule_001
name: Appointment reschedule
workflow: appointment_rescheduling

persona:
  name: Maria Lopez
  date_of_birth: 1982-04-19
  phone_number: "+15555550123"

goal:
  call_reason: rescheduling my appointment
  summary: Reschedule an existing appointment to next week.
  expected_outcome: Agent confirms a new appointment date and time.

constraints:
  allowed_facts:
    - Patient has an existing appointment.
    - Patient can attend next Tuesday or Wednesday morning.
  disallowed_behavior:
    - Do not invent insurance details.
    - Do not provide real patient data.

conversation_quality:
  welcome_behavior: Start with the configured welcome message and clearly state the rescheduling need.
  initiative: Volunteer only the next useful detail after the agent asks or pauses.
  pacing: Keep turns short and natural, with one patient fact per turn.
  clarification: If the agent asks an unclear question, ask for a simple rephrase before answering.
  expected_risks:
    - Patient may overshare before identity verification.
    - Agent may skip confirmation of the new appointment time.

steps:
  - intent: greeting
    patient_says: Hi, I need to reschedule my appointment.
  - intent: identity_verification
    patient_says: My date of birth is April 19th, 1982.
```

## Artifact Expectations

Each scenario run should eventually produce artifacts under:

```text
outputs/{call_id}/
```

Phase 2 dry runs produce:

```text
outputs/{call_id}/scenario.yaml
outputs/{call_id}/metadata.json
outputs/{call_id}/transcript.txt
outputs/{call_id}/observations.md
```

Phase 3 Retell runs produce:

```text
outputs/{call_id}/scenario.yaml
outputs/{call_id}/metadata.json
outputs/{call_id}/patient_simulation.md
outputs/{call_id}/observations.md
```

`metadata.json` links the local `call_id` to the Retell call id returned by the
outbound call API.

## Current Example

The repository includes:

```text
scenarios/appointment-reschedule.yaml
```

Run it locally with:

```powershell
.\gradlew bootRun --args="--scenario=scenarios/appointment-reschedule.yaml"
```

The generated transcript is deterministic and uses the ordered `steps` plus the
explicit `conversation_quality` guidance in the scenario file.

Start a real Retell outbound call with:

```powershell
.\gradlew bootRun --args="--scenario=scenarios/appointment-reschedule.yaml --run-mode=retell"
```

The Retell call-start path sends scenario and conversation-quality fields as
dynamic variables so the configured Retell agent can use them during the call.
Phase 5 sends these stable Retell prompt variables:

- `patient_name`
- `call_reason`
- `patient_simulation_prompt`

The generated `patient_simulation_prompt` is also written to
`outputs/{call_id}/patient_simulation.md` for review.

## Conversation Quality Fields

`conversation_quality` keeps realism guidance reviewable and scenario-owned.

- `welcome_behavior`: how the patient starts with the configured Retell welcome message
- `initiative`: how much information the patient volunteers
- `pacing`: expected turn length and rhythm
- `clarification`: how the patient handles unclear questions
- `expected_risks`: known conversation-quality issues to watch for later

## Retell Agent Prompt

The Retell agent should use a stable prompt and let this project supply the
scenario-specific behavior through dynamic variables.

Recommended Retell welcome message:

```text
Hi, this is {{patient_name}}. I'm calling about {{call_reason}}. Could you help me?
```

Recommended Retell agent prompt:

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
