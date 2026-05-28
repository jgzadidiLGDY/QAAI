package com.qaai.evaluation;

import static org.assertj.core.api.Assertions.assertThat;

import com.qaai.artifacts.NormalizedTranscript;
import com.qaai.artifacts.TranscriptTurn;
import com.qaai.scenario.Scenario;
import java.util.List;
import org.junit.jupiter.api.Test;

class EvaluationPromptBuilderTest {

	@Test
	void buildsIndependentRubricsWithHumanReviewAndEvidenceRequirements() {
		String prompt = new EvaluationPromptBuilder().build(
				scenario(),
				new NormalizedTranscript(
						"call_20260528_112500_eval",
						"appointment_reschedule_001",
						"retell",
						List.of(
								new TranscriptTurn(1, "patient", "I need to reschedule my appointment.", 0.5),
								new TranscriptTurn(2, "receptionist", "I can help with that.", 1.0)
						)
				)
		);

		assertThat(prompt).contains("Set human_review_required to true");
		assertThat(prompt).contains("Do not make authoritative pass/fail decisions");
		assertThat(prompt).contains("If evidence is weak or missing");
		assertThat(prompt).contains("- safety:");
		assertThat(prompt).contains("- accuracy:");
		assertThat(prompt).contains("- empathy:");
		assertThat(prompt).contains("- policy:");
		assertThat(prompt).contains("- workflow_completion:");
		assertThat(prompt).contains("1. [patient] I need to reschedule my appointment.");
		assertThat(prompt).contains("2. [receptionist] I can help with that.");
	}

	private Scenario scenario() {
		return new Scenario(
				"appointment_reschedule_001",
				"Appointment reschedule",
				"appointment_rescheduling",
				null,
				null,
				null,
				null,
				null,
				List.of()
		);
	}
}
