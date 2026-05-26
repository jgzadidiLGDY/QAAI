package com.qaai.scenario;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class PatientSimulationPromptBuilderTest {

	private final PatientSimulationPromptBuilder builder = new PatientSimulationPromptBuilder();

	@Test
	void buildsDeterministicPromptFromScenario() {
		Scenario scenario = new Scenario(
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
				new Scenario.ConversationQuality(
						"Wait briefly for the greeting.",
						"Volunteer one useful detail at a time.",
						"Keep turns short.",
						"Ask for a rephrase.",
						List.of("Agent may skip confirmation.")
				),
				List.of(new Scenario.Step("greeting", "Hi, I need to reschedule my appointment."))
		);

		String prompt = builder.build(scenario);

		assertThat(prompt).contains(
				"# Patient Simulation Scenario",
				"Scenario ID: appointment_reschedule_001",
				"- Call reason: rescheduling my appointment",
				"- Patient has an existing appointment.",
				"- Do not invent insurance details.",
				"1. Hi, I need to reschedule my appointment. [intent: greeting]",
				"Do not claim success unless the success condition is clearly met.",
				"If the receptionist gives a vague response or leaves a pause",
				"Avoid front-loading facts."
		);
	}
}
