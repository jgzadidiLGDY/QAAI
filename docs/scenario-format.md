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
  welcome_behavior: Wait briefly for the agent greeting, then clearly state the rescheduling need.
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

## Conversation Quality Fields

`conversation_quality` keeps realism guidance reviewable and scenario-owned.

- `welcome_behavior`: how the patient starts after the agent greeting
- `initiative`: how much information the patient volunteers
- `pacing`: expected turn length and rhythm
- `clarification`: how the patient handles unclear questions
- `expected_risks`: known conversation-quality issues to watch for later
