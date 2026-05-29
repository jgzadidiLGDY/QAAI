package com.qaai.reporting;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qaai.analysis.AnalysisFinding;
import com.qaai.analysis.AnalysisReport;
import com.qaai.analysis.EvidenceReference;
import com.qaai.artifacts.ArtifactCompletenessChecker;
import com.qaai.artifacts.ArtifactPaths;
import com.qaai.artifacts.RunIndexWriter;
import com.qaai.artifacts.RunMetadata;
import com.qaai.evaluation.EvaluationDimensionResult;
import com.qaai.evaluation.EvaluationEvidenceReference;
import com.qaai.evaluation.EvaluationReport;
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

class ReportGenerationServiceTest {

	@TempDir
	private Path tempDir;

	@Test
	void generatesStaticReportFromTrustedArtifacts() throws Exception {
		Path outputs = tempDir.resolve("outputs");
		Path scenarios = tempDir.resolve("scenarios");
		String callId = "call_20260528_120000_report1234";
		Path runDirectory = outputs.resolve(callId);
		Files.createDirectories(runDirectory);
		Files.createDirectories(scenarios);
		writeScenario(scenarios.resolve("appointment-reschedule.yaml"));
		ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
		RunMetadata metadata = metadata(callId, runDirectory);
		objectMapper.writerWithDefaultPrettyPrinter().writeValue(
				runDirectory.resolve("metadata.json").toFile(),
				metadata
		);
		Files.writeString(runDirectory.resolve("scenario.yaml"), "id: appointment_reschedule_001%n".formatted());
		Files.writeString(runDirectory.resolve("transcript.txt"), "1. [receptionist] I can put in a request.%n"
				.formatted());
		Files.writeString(runDirectory.resolve("transcript.json"), "{\"turns\":[]}");
		Files.writeString(runDirectory.resolve("patient_simulation.md"), "prompt");
		Files.writeString(runDirectory.resolve("observations.md"), "observations");
		Files.writeString(runDirectory.resolve("manifest.json"), "{}");
		Files.writeString(runDirectory.resolve("analysis.md"), "# Analysis");
		Files.writeString(runDirectory.resolve("evaluation.md"), "# Evaluation");
		objectMapper.writerWithDefaultPrettyPrinter().writeValue(
				runDirectory.resolve("analysis.json").toFile(),
				new AnalysisReport(
						callId,
						"appointment_reschedule_001",
						"Summary.",
						true,
						List.of(new AnalysisFinding(
								"Missing confirmation",
								"medium",
								"appointment_rescheduling",
								"Confirm a time.",
								"No confirmation.",
								List.of(new EvidenceReference(
										"transcript.txt",
										"receptionist",
										"I can put in a request.",
										1.0
								))
						)),
						List.of()
				)
		);
		objectMapper.writerWithDefaultPrettyPrinter().writeValue(
				runDirectory.resolve("evaluation.json").toFile(),
				new EvaluationReport(
						callId,
						"appointment_reschedule_001",
						true,
						List.of(
								new EvaluationDimensionResult(
										"workflow_completion",
										2,
										"1-5",
										"Ambiguous next step.",
										"medium",
										false,
										List.of(new EvaluationEvidenceReference(
												"transcript.txt",
												"receptionist",
												"I can put in a request.",
												1
										))
								),
								new EvaluationDimensionResult(
										"safety",
										null,
										"1-5",
										"Safety evidence is insufficient.",
										"high",
										true,
										List.of()
								)
						),
						List.of()
				)
		);
		RunIndexWriter runIndexWriter = new RunIndexWriter(objectMapper, outputs, new ArtifactCompletenessChecker());
		runIndexWriter.append(metadata);
		ReportGenerationService service = service(outputs, scenarios, runIndexWriter);

		ReportResult result = service.generate();

		assertThat(result.reportId()).isEqualTo("report_20260528_160000");
		assertThat(result.reportJson()).exists();
		assertThat(result.reportMarkdown()).exists();
		assertThat(result.reportHtml()).exists();
		assertThat(Files.readString(result.reportMarkdown())).contains(
				"# QA Static Report",
				"human_review_required: true",
				"| Call ID | Scenario | Mode | Channel | Status | Complete | Artifacts |",
				"appointment_reschedule_001",
				"voice",
				"workflow_completion",
				"2.00",
				"safety",
				"does not create pass/fail decisions"
		);
		assertThat(Files.readString(result.reportJson())).contains(
				"\"severity_counts\"",
				"\"channel\" : \"voice\"",
				"\"medium\" : 1",
				"\"workflow_area\" : \"appointment_rescheduling\"",
				"\"insufficient_evidence_count\" : 1"
		);
	}

	@Test
	void generatesEmptyReportWhenNoRunsExist() throws Exception {
		Path outputs = tempDir.resolve("outputs");
		Path scenarios = tempDir.resolve("scenarios");
		Files.createDirectories(scenarios);
		RunIndexWriter runIndexWriter = new RunIndexWriter(
				new ObjectMapper(),
				outputs,
				new ArtifactCompletenessChecker()
		);
		ReportGenerationService service = service(outputs, scenarios, runIndexWriter);

		ReportResult result = service.generate();

		assertThat(Files.readString(result.reportMarkdown())).contains(
				"No runs found.",
				"No evaluation artifacts found.",
				"No analysis findings found."
		);
	}

	private ReportGenerationService service(Path outputs, Path scenarios, RunIndexWriter runIndexWriter) {
		return new ReportGenerationService(
				new ObjectMapper(),
				outputs,
				scenarios,
				runIndexWriter,
				new ArtifactCompletenessChecker(),
				new ScenarioLoader(),
				new StaticReportRenderer(),
				Clock.fixed(Instant.parse("2026-05-28T16:00:00Z"), ZoneOffset.UTC)
		);
	}

	private RunMetadata metadata(String callId, Path runDirectory) {
		return new RunMetadata(
				callId,
				"appointment_reschedule_001",
				"retell",
				"+18054398008",
				"retell_call_123",
				OffsetDateTime.parse("2026-05-28T11:40:00-04:00"),
				OffsetDateTime.parse("2026-05-28T11:42:00-04:00"),
				"evaluation_completed",
				new ArtifactPaths(
						runDirectory.resolve("scenario.yaml").toString(),
						runDirectory.resolve("metadata.json").toString(),
						runDirectory.resolve("transcript.txt").toString(),
						runDirectory.resolve("transcript.json").toString(),
						runDirectory.resolve("patient_simulation.md").toString(),
						null,
						runDirectory.resolve("manifest.json").toString(),
						runDirectory.resolve("analysis.json").toString(),
						runDirectory.resolve("analysis.md").toString(),
						runDirectory.resolve("evaluation.json").toString(),
						runDirectory.resolve("evaluation.md").toString(),
						runDirectory.resolve("observations.md").toString()
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
}
