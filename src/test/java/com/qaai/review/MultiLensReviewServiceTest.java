package com.qaai.review;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qaai.artifacts.AnalysisMetadata;
import com.qaai.artifacts.ArtifactManifest;
import com.qaai.artifacts.ArtifactPaths;
import com.qaai.artifacts.ArtifactWriter;
import com.qaai.artifacts.EvaluationMetadata;
import com.qaai.artifacts.NormalizedTranscript;
import com.qaai.artifacts.RunMetadata;
import com.qaai.artifacts.TranscriptTurn;
import com.qaai.scenario.ScenarioLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MultiLensReviewServiceTest {

	@TempDir
	private Path tempDir;

	@Test
	void reviewsCapturedCallAndWritesEvidenceLinkedArtifacts() throws Exception {
		Path outputs = tempDir.resolve("outputs");
		String callId = "call_20260529_101500_review123";
		Path runDirectory = outputs.resolve(callId);
		writeCapturedRun(outputs, callId);
		MultiLensReviewService service = new MultiLensReviewService(
				new ArtifactWriter(new ObjectMapper(), outputs),
				new ScenarioLoader(),
				recordingClient(callId),
				fixedClock()
		);

		MultiLensReviewResult result = service.review(callId);

		assertThat(result.metadata().status()).isEqualTo("multi_lens_review_completed");
		assertThat(result.reviewJson()).exists();
		assertThat(result.reviewMarkdown()).exists();
		assertThat(Files.readString(runDirectory.resolve("multi-lens-review.json"))).contains(
				"\"call_id\" : \"call_20260529_101500_review123\"",
				"\"human_review_required\" : true",
				"\"lens_id\" : \"safety\"",
				"\"lens_id\" : \"workflow_risk\"",
				"\"quote\" : \"I can put in a request.\""
		);
		assertThat(Files.readString(runDirectory.resolve("multi-lens-review.md"))).contains(
				"# Structured Multi-Lens Review",
				"human_review_required: true",
				"Multi-lens review is advisory"
		);
		assertThat(Files.readString(runDirectory.resolve("metadata.json"))).contains(
				"\"status\" : \"multi_lens_review_completed\"",
				"\"multi_lens_review_json\"",
				"\"multi_lens_review_markdown\"",
				"\"command\" : \"multi-lens-review\""
		);
		assertThat(Files.readString(runDirectory.resolve("manifest.json"))).contains(
				"\"name\" : \"multi_lens_review_json\"",
				"Generated during Phase 18 structured multi-lens review."
		);
	}

	@Test
	void preservesExistingAnalysisAndEvaluationMetadata() throws Exception {
		Path outputs = tempDir.resolve("outputs");
		String callId = "call_20260529_101500_review123";
		Path runDirectory = outputs.resolve(callId);
		writeCapturedRun(outputs, callId);
		ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
		RunMetadata existingMetadata = objectMapper.readValue(
				runDirectory.resolve("metadata.json").toFile(),
				RunMetadata.class
		);
		objectMapper.writerWithDefaultPrettyPrinter().writeValue(
				runDirectory.resolve("metadata.json").toFile(),
				new RunMetadata(
						existingMetadata.callId(),
						existingMetadata.scenarioId(),
						existingMetadata.runMode(),
						existingMetadata.channel(),
						existingMetadata.targetPhoneNumber(),
						existingMetadata.retellCallId(),
						existingMetadata.startedAt(),
						existingMetadata.endedAt(),
						91L,
						existingMetadata.status(),
						new ArtifactPaths(
								existingMetadata.artifactPaths().scenario(),
								existingMetadata.artifactPaths().metadata(),
								existingMetadata.artifactPaths().transcriptText(),
								existingMetadata.artifactPaths().transcriptJson(),
								existingMetadata.artifactPaths().patientSimulation(),
								existingMetadata.artifactPaths().audio(),
								existingMetadata.artifactPaths().manifest(),
								runDirectory.resolve("analysis.json").toString(),
								runDirectory.resolve("analysis.md").toString(),
								runDirectory.resolve("evaluation.json").toString(),
								runDirectory.resolve("evaluation.md").toString(),
								null,
								null,
								existingMetadata.artifactPaths().observationsMarkdown()
						),
						new AnalysisMetadata("local", "deterministic-v1"),
						new EvaluationMetadata("local", "deterministic-v1"),
						existingMetadata.reproducibility()
				)
		);
		MultiLensReviewService service = new MultiLensReviewService(
				new ArtifactWriter(new ObjectMapper(), outputs),
				new ScenarioLoader(),
				recordingClient(callId),
				fixedClock()
		);

		service.review(callId);

		RunMetadata updatedMetadata = objectMapper.readValue(
				runDirectory.resolve("metadata.json").toFile(),
				RunMetadata.class
		);
		assertThat(updatedMetadata.callDurationSeconds()).isEqualTo(91L);
		assertThat(updatedMetadata.analysis()).isEqualTo(new AnalysisMetadata("local", "deterministic-v1"));
		assertThat(updatedMetadata.evaluation()).isEqualTo(new EvaluationMetadata("local", "deterministic-v1"));
		assertThat(updatedMetadata.artifactPaths().analysisJson())
				.isEqualTo(runDirectory.resolve("analysis.json").toString());
		assertThat(updatedMetadata.artifactPaths().evaluationJson())
				.isEqualTo(runDirectory.resolve("evaluation.json").toString());
	}

	@Test
	void rejectsFindingsWithoutEvidence() throws Exception {
		String callId = "call_20260529_101500_review123";
		writeCapturedRun(tempDir.resolve("outputs"), callId);
		MultiLensReviewService service = new MultiLensReviewService(
				new ArtifactWriter(new ObjectMapper(), tempDir.resolve("outputs")),
				new ScenarioLoader(),
				request -> new MultiLensReviewReport(
						callId,
						"appointment_reschedule_001",
						request.reviewId(),
						request.generatedAt(),
						"test-provider",
						"test-model",
						true,
						List.of(new ReviewLensResult(
								"safety",
								"Safety",
								"reviewed",
								"Missing evidence.",
								List.of(new ReviewFinding(
										"safety_missing_evidence",
										"medium",
										"Finding has no evidence.",
										List.of()
								)),
								List.of()
						)),
						List.of()
				),
				fixedClock()
		);

		assertThatThrownBy(() -> service.review(callId))
				.isInstanceOf(MultiLensReviewException.class)
				.hasMessageContaining("missing transcript evidence");
	}

	@Test
	void rejectsEvidenceQuotesNotFoundInTranscript() throws Exception {
		String callId = "call_20260529_101500_review123";
		writeCapturedRun(tempDir.resolve("outputs"), callId);
		MultiLensReviewService service = new MultiLensReviewService(
				new ArtifactWriter(new ObjectMapper(), tempDir.resolve("outputs")),
				new ScenarioLoader(),
				request -> new MultiLensReviewReport(
						callId,
						"appointment_reschedule_001",
						request.reviewId(),
						request.generatedAt(),
						"test-provider",
						"test-model",
						true,
						List.of(new ReviewLensResult(
								"workflow_risk",
								"Workflow risk",
								"reviewed",
								"Unsupported quote.",
								List.of(new ReviewFinding(
										"workflow_unsupported_quote",
										"medium",
										"Finding cites unavailable text.",
										List.of(new ReviewEvidenceReference(
												"transcript.txt",
												"receptionist",
												"This quote does not exist.",
												2
										))
								)),
								List.of()
						)),
						List.of()
				),
				fixedClock()
		);

		assertThatThrownBy(() -> service.review(callId))
				.isInstanceOf(MultiLensReviewException.class)
				.hasMessageContaining("Evidence quote was not found");
	}

	@Test
	void allowsExplicitInsufficientEvidenceWithoutFindings() throws Exception {
		String callId = "call_20260529_101500_review123";
		writeCapturedRun(tempDir.resolve("outputs"), callId);
		MultiLensReviewService service = new MultiLensReviewService(
				new ArtifactWriter(new ObjectMapper(), tempDir.resolve("outputs")),
				new ScenarioLoader(),
				request -> new MultiLensReviewReport(
						callId,
						"appointment_reschedule_001",
						request.reviewId(),
						request.generatedAt(),
						"test-provider",
						"test-model",
						true,
						List.of(new ReviewLensResult(
								"patient_realism",
								"Patient realism",
								"insufficient_evidence",
								"Patient realism cannot be judged from this transcript.",
								List.of(),
								List.of("More patient turns are needed.")
						)),
						List.of()
				),
				fixedClock()
		);

		MultiLensReviewResult result = service.review(callId);

		assertThat(result.reviewJson()).exists();
		assertThat(Files.readString(result.reviewJson())).contains(
				"\"status\" : \"insufficient_evidence\"",
				"Patient realism cannot be judged"
		);
	}

	@Test
	void requiresCapturedTranscriptJson() throws Exception {
		Path outputs = tempDir.resolve("outputs");
		String callId = "call_20260529_101500_review123";
		Path runDirectory = outputs.resolve(callId);
		Files.createDirectories(runDirectory);
		writeScenario(runDirectory.resolve("scenario.yaml"));
		new ObjectMapper().findAndRegisterModules().writerWithDefaultPrettyPrinter().writeValue(
				runDirectory.resolve("metadata.json").toFile(),
				metadata(callId, runDirectory, null)
		);
		MultiLensReviewService service = new MultiLensReviewService(
				new ArtifactWriter(new ObjectMapper(), outputs),
				new ScenarioLoader(),
				recordingClient(callId),
				fixedClock()
		);

		assertThatThrownBy(() -> service.review(callId))
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
						OffsetDateTime.parse("2026-05-29T10:15:00-04:00"),
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
				OffsetDateTime.parse("2026-05-29T10:15:00-04:00"),
				OffsetDateTime.parse("2026-05-29T10:17:00-04:00"),
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

	private MultiLensReviewReport report(String callId, MultiLensReviewRequest request) {
		return new MultiLensReviewReport(
				callId,
				"appointment_reschedule_001",
				request.reviewId(),
				request.generatedAt(),
				"test-provider",
				"test-model",
				true,
				ReviewLens.supported().stream()
						.map(lens -> new ReviewLensResult(
								lens.id(),
								lens.label(),
								"reviewed",
								"The lens has transcript evidence for human inspection.",
								List.of(new ReviewFinding(
										lens.id() + "_finding",
										"info",
										"Review lens observed available receptionist evidence.",
										List.of(new ReviewEvidenceReference(
												"transcript.txt",
												"receptionist",
												"I can put in a request.",
												2
										))
								)),
								List.of()
						))
						.toList(),
				List.of("Human review should confirm lens findings.")
		);
	}

	private MultiLensReviewClient recordingClient(String callId) {
		return new MultiLensReviewClient() {
			@Override
			public MultiLensReviewReport review(MultiLensReviewRequest request) {
				return report(callId, request);
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

	private Clock fixedClock() {
		return Clock.fixed(Instant.parse("2026-05-29T14:15:00Z"), ZoneOffset.ofHours(-4));
	}
}
