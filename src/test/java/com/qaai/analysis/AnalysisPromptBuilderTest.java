package com.qaai.analysis;

import static org.assertj.core.api.Assertions.assertThat;

import com.qaai.artifacts.NormalizedTranscript;
import com.qaai.artifacts.TranscriptTurn;
import com.qaai.scenario.Scenario;
import java.util.List;
import org.junit.jupiter.api.Test;

class AnalysisPromptBuilderTest {

	@Test
	void buildsPromptWithScenarioExpectationsTranscriptAndEvidenceRules() {
		Scenario scenario = scenario();
		NormalizedTranscript transcript = new NormalizedTranscript(
				"call_20260523_130000_test1234",
				"appointment_reschedule_001",
				"retell",
				List.of(
						new TranscriptTurn(1, "patient", "Hi, I need to reschedule my appointment.", 0.5),
						new TranscriptTurn(2, "receptionist", "I can help with that.", 1.0)
				)
		);

		String prompt = new AnalysisPromptBuilder().build(scenario, transcript);

		assertThat(prompt).contains(
				"Do not make an authoritative pass/fail decision",
				"Every finding must cite exact transcript evidence",
				"scenario_id: appointment_reschedule_001",
				"expected_outcome: Agent confirms a new appointment date and time.",
				"1. speaker=patient timestamp=0.5 text=Hi, I need to reschedule my appointment.",
				"2. speaker=receptionist timestamp=1.0 text=I can help with that.",
				"\"speaker\": \"patient|receptionist|unknown\"",
				"\"human_review_required\": true"
		);
	}

	private Scenario scenario() {
		return new Scenario(
				"appointment_reschedule_001",
				"Appointment reschedule",
				"appointment_rescheduling",
				new Scenario.Persona("Alex Patient", "1980-01-01", "+15555550100"),
				new Scenario.Goal(
						"rescheduling my appointment",
						"Patient needs to reschedule an appointment.",
						"Agent confirms a new appointment date and time."
				),
				new Scenario.Constraints(List.of("Patient has an appointment."), List.of("Do not invent insurance details.")),
				new Scenario.Coverage(
						"appointment_rescheduling",
						List.of("happy_path"),
						"Confirm a new appointment time."
				),
				new Scenario.ConversationQuality(
						"Open clearly.",
						"Ask a follow-up if needed.",
						"Keep responses short.",
						"Clarify confusion.",
						List.of("Agent may skip confirmation.")
				),
				List.of(new Scenario.Step("greeting", "Hi, I need to reschedule my appointment."))
		);
	}
}
