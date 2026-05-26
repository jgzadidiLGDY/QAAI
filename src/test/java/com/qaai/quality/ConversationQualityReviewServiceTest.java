package com.qaai.quality;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qaai.artifacts.ArtifactPaths;
import com.qaai.artifacts.ArtifactWriter;
import com.qaai.artifacts.NormalizedTranscript;
import com.qaai.artifacts.RunMetadata;
import com.qaai.artifacts.TranscriptTurn;
import com.qaai.scenario.ScenarioLoader;
import com.qaai.scenario.ScenarioValidator;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ConversationQualityReviewServiceTest {

	@TempDir
	private Path tempDir;

	@Test
	void writesGroundedObservationsFromCapturedTranscript() throws Exception {
		Path outputs = tempDir.resolve("outputs");
		String callId = "call_20260523_130000_test1234";
		Path runDirectory = outputs.resolve(callId);
		Path sourceScenario = writeScenario(tempDir.resolve("scenario.yaml"));
		ArtifactWriter writer = new ArtifactWriter(new ObjectMapper(), outputs);
		writer.writeCallStartedArtifacts(
				callId,
				sourceScenario,
				retellMetadata(callId, runDirectory),
				"# Patient Simulation Scenario%n".formatted(),
				"# Previous Observations%n".formatted()
		);
		new ObjectMapper().findAndRegisterModules().writerWithDefaultPrettyPrinter().writeValue(
				runDirectory.resolve("transcript.json").toFile(),
				new NormalizedTranscript(
						callId,
						"appointment_reschedule_001",
						"retell",
						List.of(
								new TranscriptTurn(1, "patient", "Hi, I need to reschedule my appointment.", 0.5),
								new TranscriptTurn(2, "receptionist", "What is your date of birth?", 1.0),
								new TranscriptTurn(3, "patient", "My date of birth is April 19th, 1982.", 2.0),
								new TranscriptTurn(4, "patient", "Sorry, could you rephrase that?", 3.0),
								new TranscriptTurn(5, "receptionist", "I can offer next Tuesday morning.", 4.0)
						)
				)
		);
		ConversationQualityReviewService service = new ConversationQualityReviewService(
				writer,
				new ScenarioLoader(),
				new ScenarioValidator()
		);

		ConversationQualityReviewResult result = service.review(callId);

		assertThat(result.observationsMarkdown()).exists();
		assertThat(Files.readString(result.observationsMarkdown())).contains(
				"# Conversation Quality Observations",
				"## Scenario Guidance",
				"## Welcome Behavior",
				"turn 1 [patient] Hi, I need to reschedule my appointment.",
				"## Initiative And Over-Sharing",
				"turn 4 [patient] Sorry, could you rephrase that?",
				"## Clarification And Confusion Recovery",
				"## Workflow Movement",
				"Late-call evidence: turn 5 [receptionist] I can offer next Tuesday morning.",
				"Do not treat this artifact as an automated pass/fail decision."
		);
		assertThat(Files.readString(outputs.resolve("index.jsonl"))).contains(
				"\"call_id\":\"call_20260523_130000_test1234\""
		);
	}

	@Test
	void recordsTranscriptUnavailableWithoutInventingEvidence() throws Exception {
		Path outputs = tempDir.resolve("outputs");
		String callId = "call_20260523_130000_test1234";
		Path runDirectory = outputs.resolve(callId);
		Path sourceScenario = writeScenario(tempDir.resolve("scenario.yaml"));
		ArtifactWriter writer = new ArtifactWriter(new ObjectMapper(), outputs);
		writer.writeCallStartedArtifacts(
				callId,
				sourceScenario,
				retellMetadata(callId, runDirectory),
				"# Patient Simulation Scenario%n".formatted(),
				"# Previous Observations%n".formatted()
		);
		ConversationQualityReviewService service = new ConversationQualityReviewService(
				writer,
				new ScenarioLoader(),
				new ScenarioValidator()
		);

		ConversationQualityReviewResult result = service.review(callId);

		assertThat(Files.readString(result.observationsMarkdown())).contains(
				"Transcript evidence: not available",
				"Human reviewer should not infer conversation quality without transcript evidence.",
				"Pending human review."
		);
	}

	@Test
	void rejectsMissingCallId() {
		ConversationQualityReviewService service = new ConversationQualityReviewService(
				new ArtifactWriter(new ObjectMapper(), tempDir.resolve("outputs")),
				new ScenarioLoader(),
				new ScenarioValidator()
		);

		assertThatThrownBy(() -> service.review(" "))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("--review-conversation");
	}

	private Path writeScenario(Path path) throws Exception {
		Files.writeString(path, """
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
				  expected_outcome: Receptionist confirms a new appointment date and time.

				constraints:
				  allowed_facts:
				    - Patient has an existing appointment.
				    - Patient can attend next Tuesday or Wednesday morning.
				  disallowed_behavior:
				    - Do not invent insurance details.

				conversation_quality:
				  welcome_behavior: Clearly state the rescheduling need.
				  initiative: Volunteer one useful detail at a time.
				  pacing: Keep turns short.
				  clarification: Ask for a simple rephrase.
				  expected_risks:
				    - Patient may overshare before identity verification.

				steps:
				  - intent: greeting
				    patient_says: Hi, I need to reschedule my appointment.
				""");
		return path;
	}

	private RunMetadata retellMetadata(String callId, Path runDirectory) {
		return new RunMetadata(
				callId,
				"appointment_reschedule_001",
				"retell",
				"+18054398008",
				"retell_call_123",
				OffsetDateTime.parse("2026-05-23T13:00:00-04:00"),
				OffsetDateTime.parse("2026-05-23T13:00:01-04:00"),
				"retell_registered",
				new ArtifactPaths(
						runDirectory.resolve("scenario.yaml").toString(),
						runDirectory.resolve("metadata.json").toString(),
						null,
						null,
						runDirectory.resolve("patient_simulation.md").toString(),
						null,
						null,
						null,
						null,
						runDirectory.resolve("observations.md").toString()
				)
		);
	}
}
