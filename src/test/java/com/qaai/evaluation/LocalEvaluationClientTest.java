package com.qaai.evaluation;

import static org.assertj.core.api.Assertions.assertThat;

import com.qaai.artifacts.NormalizedTranscript;
import com.qaai.artifacts.TranscriptTurn;
import com.qaai.scenario.Scenario;
import java.util.List;
import org.junit.jupiter.api.Test;

class LocalEvaluationClientTest {

	@Test
	void producesDeterministicAdvisoryDimensionsWithEvidence() {
		EvaluationReport report = new LocalEvaluationClient().evaluate(new EvaluationRequest(
				scenario(),
				new NormalizedTranscript(
						"call_20260528_112500_eval",
						"appointment_reschedule_001",
						"retell",
						List.of(
								new TranscriptTurn(1, "patient", "I need to reschedule my appointment.", 0.5),
								new TranscriptTurn(2, "receptionist", "I can help with that.", 1.0)
						)
				),
				"prompt ignored by local evaluator"
		));

		assertThat(report.callId()).isEqualTo("call_20260528_112500_eval");
		assertThat(report.scenarioId()).isEqualTo("appointment_reschedule_001");
		assertThat(report.humanReviewRequired()).isTrue();
		assertThat(report.dimensions()).hasSize(5);
		assertThat(report.dimensions()).extracting(EvaluationDimensionResult::name)
				.containsExactly("safety", "accuracy", "empathy", "policy", "workflow_completion");
		assertThat(report.dimensions()).allSatisfy(dimension -> {
			assertThat(dimension.score()).isEqualTo(3);
			assertThat(dimension.scale()).isEqualTo("1-5");
			assertThat(dimension.uncertainty()).isEqualTo("medium");
			assertThat(dimension.insufficientEvidence()).isFalse();
			assertThat(dimension.evidence()).containsExactly(new EvaluationEvidenceReference(
					"transcript.txt",
					"receptionist",
					"I can help with that.",
					2
			));
		});
	}

	@Test
	void recordsInsufficientEvidenceInsteadOfGuessingWithoutReceptionistTurns() {
		EvaluationReport report = new LocalEvaluationClient().evaluate(new EvaluationRequest(
				scenario(),
				new NormalizedTranscript(
						"call_20260528_112500_eval",
						"appointment_reschedule_001",
						"retell",
						List.of(new TranscriptTurn(1, "patient", "Hello?", 0.5))
				),
				"prompt ignored by local evaluator"
		));

		assertThat(report.dimensions()).allSatisfy(dimension -> {
			assertThat(dimension.score()).isNull();
			assertThat(dimension.insufficientEvidence()).isTrue();
			assertThat(dimension.evidence()).isEmpty();
			assertThat(dimension.uncertainty()).isEqualTo("high");
		});
	}

	@Test
	void exposesDeterministicProviderIdentity() {
		LocalEvaluationClient client = new LocalEvaluationClient();

		assertThat(client.provider()).isEqualTo("local");
		assertThat(client.model()).isEqualTo("deterministic-v1");
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
