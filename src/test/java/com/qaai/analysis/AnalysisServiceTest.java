package com.qaai.analysis;

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

class AnalysisServiceTest {

	@TempDir
	private Path tempDir;

	@Test
	void analyzesCapturedCallAndWritesEvidenceLinkedArtifacts() throws Exception {
		Path outputs = tempDir.resolve("outputs");
		String callId = "call_20260523_130000_test1234";
		Path runDirectory = outputs.resolve(callId);
		writeCapturedRun(outputs, callId);
		ArtifactWriter writer = new ArtifactWriter(new ObjectMapper(), outputs);
		AnalysisService service = new AnalysisService(
				writer,
				new ScenarioLoader(),
				new AnalysisPromptBuilder(),
				prompt -> report(callId)
		);

		AnalysisResult result = service.analyze(callId);

		assertThat(result.metadata().status()).isEqualTo("analysis_completed");
		assertThat(result.analysisJson()).exists();
		assertThat(result.analysisMarkdown()).exists();
		assertThat(Files.readString(runDirectory.resolve("analysis.json"))).contains(
				"\"call_id\" : \"call_20260523_130000_test1234\"",
				"\"human_review_required\" : true",
				"Agent did not confirm the new appointment time",
				"\"quote\" : \"I can put in a request.\""
		);
		assertThat(Files.readString(runDirectory.resolve("analysis.md"))).contains(
				"# AI-Assisted Analysis",
				"human_review_required: true",
				"AI findings are advisory"
		);
		assertThat(Files.readString(runDirectory.resolve("metadata.json"))).contains(
				"\"status\" : \"analysis_completed\"",
				"\"analysis_json\"",
				"\"analysis_markdown\""
		);
		assertThat(Files.readString(runDirectory.resolve("manifest.json"))).contains(
				"\"name\" : \"analysis_json\"",
				"Generated during Phase 6 AI-assisted analysis."
		);
	}

	@Test
	void rejectsFindingsWithoutEvidence() throws Exception {
		String callId = "call_20260523_130000_test1234";
		writeCapturedRun(tempDir.resolve("outputs"), callId);
		ArtifactWriter writer = new ArtifactWriter(new ObjectMapper(), tempDir.resolve("outputs"));
		AnalysisService service = new AnalysisService(
				writer,
				new ScenarioLoader(),
				new AnalysisPromptBuilder(),
				prompt -> new AnalysisReport(
						callId,
						"appointment_reschedule_001",
						"Summary",
						true,
						List.of(new AnalysisFinding(
								"Unsupported issue",
								"medium",
								"appointment_rescheduling",
								"Expected behavior",
								"Actual behavior",
								List.of()
						)),
						List.of()
				)
		);

		assertThatThrownBy(() -> service.analyze(callId))
				.isInstanceOf(AnalysisException.class)
				.hasMessageContaining("missing transcript evidence");
	}

	@Test
	void rejectsEvidenceQuotesNotFoundInTranscript() throws Exception {
		String callId = "call_20260523_130000_test1234";
		writeCapturedRun(tempDir.resolve("outputs"), callId);
		ArtifactWriter writer = new ArtifactWriter(new ObjectMapper(), tempDir.resolve("outputs"));
		AnalysisService service = new AnalysisService(
				writer,
				new ScenarioLoader(),
				new AnalysisPromptBuilder(),
				prompt -> new AnalysisReport(
						callId,
						"appointment_reschedule_001",
						"Summary",
						true,
						List.of(new AnalysisFinding(
								"Unsupported issue",
								"medium",
								"appointment_rescheduling",
								"Expected behavior",
								"Actual behavior",
								List.of(new EvidenceReference("transcript.txt", "agent", "This quote does not exist.", null))
						)),
						List.of()
				)
		);

		assertThatThrownBy(() -> service.analyze(callId))
				.isInstanceOf(AnalysisException.class)
				.hasMessageContaining("Evidence quote was not found");
	}

	@Test
	void requiresCapturedTranscriptJson() throws Exception {
		Path outputs = tempDir.resolve("outputs");
		String callId = "call_20260523_130000_test1234";
		Path runDirectory = outputs.resolve(callId);
		Files.createDirectories(runDirectory);
		writeScenario(runDirectory.resolve("scenario.yaml"));
		new ObjectMapper().findAndRegisterModules().writerWithDefaultPrettyPrinter().writeValue(
				runDirectory.resolve("metadata.json").toFile(),
				metadata(callId, runDirectory, null)
		);
		AnalysisService service = new AnalysisService(
				new ArtifactWriter(new ObjectMapper(), outputs),
				new ScenarioLoader(),
				new AnalysisPromptBuilder(),
				prompt -> report(callId)
		);

		assertThatThrownBy(() -> service.analyze(callId))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("transcript_json");
	}

	@Test
	void rejectsCallIdWithPathCharactersBeforeResolvingArtifacts() {
		AnalysisService service = new AnalysisService(
				new ArtifactWriter(new ObjectMapper(), tempDir.resolve("outputs")),
				new ScenarioLoader(),
				new AnalysisPromptBuilder(),
				prompt -> report("unused")
		);

		assertThatThrownBy(() -> service.analyze("<retell_call_id_without_transcript>"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("call_id may contain only letters");
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
								new TranscriptTurn(2, "agent", "I can put in a request.", 1.0)
						)
				)
		);
		Files.writeString(runDirectory.resolve("transcript.txt"), "1. [patient] Hi%n2. [agent] I can put in a request.%n".formatted());
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
						OffsetDateTime.parse("2026-05-23T18:30:00-04:00"),
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
				OffsetDateTime.parse("2026-05-23T13:00:00-04:00"),
				OffsetDateTime.parse("2026-05-23T13:10:00-04:00"),
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

	private AnalysisReport report(String callId) {
		return new AnalysisReport(
				callId,
				"appointment_reschedule_001",
				"The patient attempted to reschedule an appointment.",
				true,
				List.of(new AnalysisFinding(
						"Agent did not confirm the new appointment time",
						"medium",
						"appointment_rescheduling",
						"Agent confirms a new appointment date and time.",
						"Agent only said a request would be entered.",
						List.of(new EvidenceReference("transcript.txt", "agent", "I can put in a request.", 1.0))
				)),
				List.of("Human review should confirm whether this workflow later completed elsewhere.")
		);
	}
}
