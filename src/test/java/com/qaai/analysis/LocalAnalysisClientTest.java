package com.qaai.analysis;

import static org.assertj.core.api.Assertions.assertThat;

import com.qaai.artifacts.NormalizedTranscript;
import com.qaai.artifacts.TranscriptTurn;
import com.qaai.scenario.Scenario;
import java.util.List;
import org.junit.jupiter.api.Test;

class LocalAnalysisClientTest {

	@Test
	void producesDeterministicHumanReviewedReportWithoutNetwork() {
		LocalAnalysisClient client = new LocalAnalysisClient();
		AnalysisReport report = client.analyze(new AnalysisRequest(
				scenario(),
				new NormalizedTranscript(
						"call_20260526_150000_local",
						"appointment_reschedule_001",
						"retell",
						List.of(
								new TranscriptTurn(1, "patient", "I need to reschedule.", 0.5),
								new TranscriptTurn(2, "receptionist", "I can help with that.", 1.0)
						)
				),
				"prompt ignored by local analyzer"
		));

		assertThat(report.callId()).isEqualTo("call_20260526_150000_local");
		assertThat(report.scenarioId()).isEqualTo("appointment_reschedule_001");
		assertThat(report.summary()).isEqualTo(
				"Deterministic local analysis reviewed 2 transcript turns: 1 patient and 1 receptionist."
		);
		assertThat(report.humanReviewRequired()).isTrue();
		assertThat(report.findings()).isEmpty();
		assertThat(report.notes()).contains("Human review remains required for workflow judgment.");
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
				List.of()
		);
	}
}
