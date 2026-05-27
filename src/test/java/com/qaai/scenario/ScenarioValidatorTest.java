package com.qaai.scenario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class ScenarioValidatorTest {

	private final ScenarioValidator validator = new ScenarioValidator();

	@Test
	void acceptsValidScenario() {
		Scenario scenario = validScenario();

		validator.validate(scenario);
	}

	@Test
	void rejectsMissingRequiredFields() {
		Scenario scenario = new Scenario(
				"",
				null,
				" ",
				new Scenario.Persona("", null, null),
				new Scenario.Goal(null, null, null),
				null,
				null,
				new Scenario.ConversationQuality("", null, " ", null, List.of()),
				List.of(new Scenario.Step("", null))
		);

		assertThatThrownBy(() -> validator.validate(scenario))
				.isInstanceOf(ScenarioValidationException.class)
				.satisfies(exception -> assertThat(((ScenarioValidationException) exception).errors())
						.contains(
								"id is required",
								"name is required",
								"workflow is required",
								"persona.name is required",
								"goal.call_reason is required",
								"goal.summary is required",
								"goal.expected_outcome is required",
								"constraints is required",
								"coverage is required",
								"conversation_quality.welcome_behavior is required",
								"conversation_quality.initiative is required",
								"conversation_quality.pacing is required",
								"conversation_quality.clarification is required",
								"conversation_quality.expected_risks must include at least one item",
								"steps[0].intent is required",
								"steps[0].patient_says is required"
						));
	}

	@Test
	void rejectsScenarioWithoutSteps() {
		Scenario scenario = new Scenario(
				"appointment_reschedule_001",
				"Appointment reschedule",
				"appointment_rescheduling",
				new Scenario.Persona("Maria Lopez", null, null),
				new Scenario.Goal("rescheduling my appointment", "Reschedule appointment.", "Agent confirms it."),
				null,
				new Scenario.Coverage(
						"appointment_rescheduling",
						List.of("happy_path"),
						"Confirm a new appointment time."
				),
				new Scenario.ConversationQuality(
						"Wait for greeting.",
						"Answer one prompt at a time.",
						"Keep turns short.",
						"Ask for a rephrase.",
						List.of("Agent may skip confirmation.")
				),
				List.of()
		);

		assertThatThrownBy(() -> validator.validate(scenario))
				.isInstanceOf(ScenarioValidationException.class)
				.hasMessageContaining("steps must include at least one step");
	}

	@Test
	void rejectsInvalidCoverage() {
		Scenario scenario = new Scenario(
				"appointment_reschedule_001",
				"Appointment reschedule",
				"appointment_rescheduling",
				new Scenario.Persona("Maria Lopez", null, null),
				new Scenario.Goal("rescheduling my appointment", "Reschedule appointment.", "Agent confirms it."),
				new Scenario.Constraints(
						List.of("Patient has an existing appointment."),
						List.of("Do not invent insurance details.")
				),
				new Scenario.Coverage(
						"",
						List.of("happy_path", "too_easy", ""),
						" "
				),
				new Scenario.ConversationQuality(
						"Wait for greeting.",
						"Answer one prompt at a time.",
						"Keep turns short.",
						"Ask for a rephrase.",
						List.of("Agent may skip confirmation.")
				),
				List.of(new Scenario.Step("greeting", "Hi, I need to reschedule my appointment."))
		);

		assertThatThrownBy(() -> validator.validate(scenario))
				.isInstanceOf(ScenarioValidationException.class)
				.satisfies(exception -> assertThat(((ScenarioValidationException) exception).errors())
						.contains(
								"coverage.workflow_area is required",
								"coverage.risk_focus is required",
								"coverage.edge_cases[2] is required"
						)
						.anyMatch(error -> error.equals(
								"coverage.edge_cases[1] must be one of [workflow_recovery, missing_fact, "
										+ "workflow_mismatch, unavailable_information, clarification, happy_path, "
										+ "transfer_or_hold, ambiguous_next_step]"
						) || error.startsWith("coverage.edge_cases[1] must be one of ")));
	}

	private static Scenario validScenario() {
		return new Scenario(
				"appointment_reschedule_001",
				"Appointment reschedule",
				"appointment_rescheduling",
				new Scenario.Persona("Maria Lopez", "1982-04-19", "+15555550123"),
				new Scenario.Goal(
						"rescheduling my appointment",
						"Reschedule an existing appointment.",
						"Agent confirms a new appointment."
				),
				new Scenario.Constraints(
						List.of("Patient has an existing appointment."),
						List.of("Do not invent insurance details.")
				),
				new Scenario.Coverage(
						"appointment_rescheduling",
						List.of("happy_path"),
						"Confirm a new appointment time."
				),
				new Scenario.ConversationQuality(
						"Wait for greeting.",
						"Answer one prompt at a time.",
						"Keep turns short.",
						"Ask for a rephrase.",
						List.of("Agent may skip confirmation.")
				),
				List.of(new Scenario.Step("greeting", "Hi, I need to reschedule my appointment."))
		);
	}
}
