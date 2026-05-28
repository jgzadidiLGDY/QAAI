package com.qaai.evaluation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qaai.artifacts.ArtifactManifest;
import com.qaai.artifacts.ArtifactPaths;
import com.qaai.artifacts.ArtifactWriter;
import com.qaai.artifacts.NormalizedTranscript;
import com.qaai.artifacts.RunMetadata;
import com.qaai.artifacts.TranscriptTurn;
import com.qaai.scenario.ScenarioLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class EvaluationServiceTest {

	@TempDir
	private Path tempDir;

	@Test
	void evaluatesCapturedCallAndWritesEvidenceLinkedArtifacts() throws Exception {
		Path outputs = tempDir.resolve("outputs");
		String callId = "call_20260528_114000_eval1234";
		Path runDirectory = outputs.resolve(callId);
		writeCapturedRun(outputs, callId);
		EvaluationService service = new EvaluationService(
				new ArtifactWriter(new ObjectMapper(), outputs),
				new ScenarioLoader(),
				new EvaluationPromptBuilder(),
				recordingClient(callId)
		);

		EvaluationResult result = service.evaluate(callId);

		assertThat(result.metadata().status()).isEqualTo("evaluation_completed");
		assertThat(result.evaluationJson()).exists();
		assertThat(result.evaluationMarkdown()).exists();
		assertThat(Files.readString(runDirectory.resolve("evaluation.json"))).contains(
				"\"call_id\" : \"call_20260528_114000_eval1234\"",
				"\"human_review_required\" : true",
				"\"name\" : \"workflow_completion\"",
				"\"quote\" : \"I can put in a request.\""
		);
		assertThat(Files.readString(runDirectory.resolve("evaluation.md"))).contains(
				"# Evidence-Linked Evaluation",
				"human_review_required: true",
				"Evaluation scores are advisory"
		);
		assertThat(Files.readString(runDirectory.resolve("metadata.json"))).contains(
				"\"status\" : \"evaluation_completed\"",
				"\"evaluation_json\"",
				"\"evaluation_markdown\"",
				"\"evaluation\" : {",
				"\"provider\" : \"test-provider\"",
				"\"model\" : \"test-model\"",
				"\"command\" : \"evaluate-call\""
		);
		assertThat(Files.readString(runDirectory.resolve("manifest.json"))).contains(
				"\"name\" : \"evaluation_json\"",
				"Generated during Phase 15 evidence-linked evaluation."
		);
	}

	@Test
	void rejectsScoredDimensionsWithoutEvidence() throws Exception {
		String callId = "call_20260528_114000_eval1234";
		writeCapturedRun(tempDir.resolve("outputs"), callId);
		EvaluationService service = new EvaluationService(
				new ArtifactWriter(new ObjectMapper(), tempDir.resolve("outputs")),
				new ScenarioLoader(),
				new EvaluationPromptBuilder(),
				request -> new EvaluationReport(
						callId,
						"appointment_reschedule_001",
						true,
						List.of(new EvaluationDimensionResult(
								"empathy",
								4,
								"1-5",
								"Looks empathetic.",
								"low",
								false,
								List.of()
						)),
						List.of()
				)
		);

		assertThatThrownBy(() -> service.evaluate(callId))
				.isInstanceOf(EvaluationException.class)
				.hasMessageContaining("missing transcript evidence");
	}

	@Test
	void rejectsEvidenceQuotesNotFoundInTranscript() throws Exception {
		String callId = "call_20260528_114000_eval1234";
		writeCapturedRun(tempDir.resolve("outputs"), callId);
		EvaluationService service = new EvaluationService(
				new ArtifactWriter(new ObjectMapper(), tempDir.resolve("outputs")),
				new ScenarioLoader(),
				new EvaluationPromptBuilder(),
				request -> new EvaluationReport(
						callId,
						"appointment_reschedule_001",
						true,
						List.of(new EvaluationDimensionResult(
								"workflow_completion",
								2,
								"1-5",
								"Unsupported quote.",
								"medium",
								false,
								List.of(new EvaluationEvidenceReference(
										"transcript.txt",
										"receptionist",
										"This quote does not exist.",
										2
								))
						)),
						List.of()
				)
		);

		assertThatThrownBy(() -> service.evaluate(callId))
				.isInstanceOf(EvaluationException.class)
				.hasMessageContaining("Evidence quote was not found");
	}

	@Test
	void allowsExplicitInsufficientEvidenceWithoutScore() throws Exception {
		String callId = "call_20260528_114000_eval1234";
		writeCapturedRun(tempDir.resolve("outputs"), callId);
		EvaluationService service = new EvaluationService(
				new ArtifactWriter(new ObjectMapper(), tempDir.resolve("outputs")),
				new ScenarioLoader(),
				new EvaluationPromptBuilder(),
				request -> new EvaluationReport(
						callId,
						"appointment_reschedule_001",
						true,
						List.of(new EvaluationDimensionResult(
								"safety",
								null,
								"1-5",
								"Safety evidence is insufficient in this transcript.",
								"high",
								true,
								List.of()
						)),
						List.of()
				)
		);

		EvaluationResult result = service.evaluate(callId);

		assertThat(result.evaluationJson()).exists();
		assertThat(Files.readString(result.evaluationJson())).contains(
				"\"insufficient_evidence\" : true",
				"Safety evidence is insufficient"
		);
	}

	@Test
	void requiresCapturedTranscriptJson() throws Exception {
		Path outputs = tempDir.resolve("outputs");
		String callId = "call_20260528_114000_eval1234";
		Path runDirectory = outputs.resolve(callId);
		Files.createDirectories(runDirectory);
		writeScenario(runDirectory.resolve("scenario.yaml"));
		new ObjectMapper().findAndRegisterModules().writerWithDefaultPrettyPrinter().writeValue(
				runDirectory.resolve("metadata.json").toFile(),
				metadata(callId, runDirectory, null)
		);
		EvaluationService service = new EvaluationService(
				new ArtifactWriter(new ObjectMapper(), outputs),
				new ScenarioLoader(),
				new EvaluationPromptBuilder(),
				request -> report(callId)
		);

		assertThatThrownBy(() -> service.evaluate(callId))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("transcript_json");
	}

	private void writeCapturedRun(Path outputs, String callId) throws Exception {
		Path runDirectory = outputs.resolve(callId);
		Files.createDirectories(runDirectory);
		writeScenario(runDirectory.resolve("scenario.yaml"));
		ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
		objectMapper.writerWithDefaultPrettyPrinter().writeValue(
				runDirectory.resolve("transcript.json").toFile(),
				new NormalizedTranscript(
						callId,
						"appointment_reschedule_001",
						"retell",
						List.of(
								new TranscriptTurn(1, "patient", "Hi, I need to reschedule my appointment.", 0.5),
								new TranscriptTurn(2, "receptionist", "I can put in a request.", 1.0)
						)
				)
		);
		Files.writeString(runDirectory.resolve("transcript.txt"),
				"1. [patient] Hi%n2. [receptionist] I can put in a request.%n".formatted());
		objectMapper.writerWithDefaultPrettyPrinter().writeValue(
				runDirectory.resolve("metadata.json").toFile(),
				metadata(callId, runDirectory, runDirectory.resolve("transcript.json").toString())
		);
		objectMapper.writerWithDefaultPrettyPrinter().writeValue(
				runDirectory.resolve("manifest.json").toFile(),
				new ArtifactManifest(
						callId,
						"appointment_reschedule_001",
						"retell_call_123",
						OffsetDateTime.parse("2026-05-28T11:40:00-04:00"),
						"ended",
						null,
						List.of(new ArtifactManifest.ArtifactEntry(
								"transcript_json",
								runDirectory.resolve("transcript.json").toString(),
								true,
								"Generated during Phase 4 artifact capture."
						))
				)
		);
	}

	private void writeScenario(Path path) throws Exception {
		Files.writeString(path, """
				id: appointment_reschedule_001
				name: Appointment reschedule
				workflow: appointment_rescheduling
				persona:
				  name: Alex Patient
				  date_of_birth: 1980-01-01
				  phone_number: "+15555550100"
				goal:
				  call_reason: rescheduling my appointment
				  summary: Patient needs to reschedule an appointment.
				  expected_outcome: Agent confirms a new appointment date and time.
				constraints:
				  allowed_facts:
				    - Patient has an appointment.
				  disallowed_behavior:
				    - Do not invent insurance details.
				coverage:
				  workflow_area: appointment_rescheduling
				  edge_cases:
				    - happy_path
				  risk_focus: Confirm a new appointment time.
				conversation_quality:
				  welcome_behavior: Open clearly.
				  initiative: Ask a follow-up if needed.
				  pacing: Keep responses short.
				  clarification: Clarify confusion.
				  expected_risks:
				    - Agent may skip confirmation.
				steps:
				  - intent: greeting
				    patient_says: Hi, I need to reschedule my appointment.
				""");
	}

	private RunMetadata metadata(String callId, Path runDirectory, String transcriptJson) {
		return new RunMetadata(
				callId,
				"appointment_reschedule_001",
				"retell",
				"+18054398008",
				"retell_call_123",
				OffsetDateTime.parse("2026-05-28T11:40:00-04:00"),
				OffsetDateTime.parse("2026-05-28T11:42:00-04:00"),
				"artifacts_captured",
				new ArtifactPaths(
						runDirectory.resolve("scenario.yaml").toString(),
						runDirectory.resolve("metadata.json").toString(),
						runDirectory.resolve("transcript.txt").toString(),
						transcriptJson,
						runDirectory.resolve("patient_simulation.md").toString(),
						null,
						runDirectory.resolve("manifest.json").toString(),
						null,
						null,
						runDirectory.resolve("observations.md").toString()
				)
		);
	}

	private EvaluationReport report(String callId) {
		return new EvaluationReport(
				callId,
				"appointment_reschedule_001",
				true,
				List.of(new EvaluationDimensionResult(
						"workflow_completion",
						3,
						"1-5",
						"The receptionist gave a limited next step.",
						"medium",
						false,
						List.of(new EvaluationEvidenceReference(
								"transcript.txt",
								"receptionist",
								"I can put in a request.",
								2
						))
				)),
				List.of("Human review should confirm workflow outcome.")
		);
	}

	private EvaluationClient recordingClient(String callId) {
		return new EvaluationClient() {
			@Override
			public EvaluationReport evaluate(EvaluationRequest request) {
				return report(callId);
			}

			@Override
			public String provider() {
				return "test-provider";
			}

			@Override
			public String model() {
				return "test-model";
			}
		};
	}
}
