package com.qaai.runner;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qaai.artifacts.ArtifactCompletenessChecker;
import com.qaai.artifacts.ArtifactPaths;
import com.qaai.artifacts.ArtifactWriter;
import com.qaai.artifacts.RunIndexWriter;
import com.qaai.artifacts.RunMetadata;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RunInspectionServiceTest {

	@TempDir
	private Path tempDir;

	@Test
	void showsOneRunWithCompletenessAndLatestIndexEntry() throws Exception {
		Path outputs = tempDir.resolve("outputs");
		RunInspectionService service = service(outputs);
		RunMetadata metadata = metadata(
				"call_20260527_100000_alpha",
				"appointment_reschedule_001",
				"retell",
				"retell_registered",
				null
		);
		writeRunFiles(outputs, metadata);
		serviceIndex(outputs).append(metadata);

		RunInspection inspection = service.showRun("call_20260527_100000_alpha");

		assertThat(inspection.metadata().scenarioId()).isEqualTo("appointment_reschedule_001");
		assertThat(inspection.runDirectory()).isEqualTo(outputs.resolve("call_20260527_100000_alpha"));
		assertThat(inspection.completeness().complete()).isTrue();
		assertThat(inspection.latestIndexEntry()).isNotNull();
		assertThat(inspection.latestIndexEntry().status()).isEqualTo("retell_registered");
	}

	@Test
	void filtersRunIndexByScenarioStatusAndRunMode() throws Exception {
		Path outputs = tempDir.resolve("outputs");
		RunInspectionService service = service(outputs);
		RunIndexWriter writer = serviceIndex(outputs);
		RunMetadata dryRun = metadata(
				"call_20260527_100000_alpha",
				"appointment_reschedule_001",
				"dry_run",
				"completed",
				"transcript"
		);
		RunMetadata retellRun = metadata(
				"call_20260527_101500_beta",
				"billing_question_001",
				"retell",
				"retell_registered",
				null
		);
		writeRunFiles(outputs, dryRun);
		writeRunFiles(outputs, retellRun);
		writer.append(dryRun);
		writer.append(retellRun);

		assertThat(service.listRuns(new RunFilters("appointment_reschedule_001", "completed", "dry-run")))
				.extracting(entry -> entry.callId())
				.containsExactly("call_20260527_100000_alpha");
		assertThat(service.listRuns(new RunFilters(null, null, "retell")))
				.extracting(entry -> entry.callId())
				.containsExactly("call_20260527_101500_beta");
	}

	private RunInspectionService service(Path outputs) {
		RunIndexWriter runIndexWriter = serviceIndex(outputs);
		return new RunInspectionService(
				new ArtifactWriter(new ObjectMapper(), outputs, runIndexWriter),
				new ArtifactCompletenessChecker(),
				runIndexWriter
		);
	}

	private RunIndexWriter serviceIndex(Path outputs) {
		return new RunIndexWriter(new ObjectMapper(), outputs, new ArtifactCompletenessChecker());
	}

	private void writeRunFiles(Path outputs, RunMetadata metadata) throws Exception {
		Path runDirectory = outputs.resolve(metadata.callId());
		Files.createDirectories(runDirectory);
		Files.writeString(runDirectory.resolve("scenario.yaml"), "id: " + metadata.scenarioId());
		Files.writeString(runDirectory.resolve("metadata.json"),
				new ObjectMapper().findAndRegisterModules().writerWithDefaultPrettyPrinter()
						.writeValueAsString(metadata));
		Files.writeString(runDirectory.resolve("patient_simulation.md"), "patient");
		Files.writeString(runDirectory.resolve("observations.md"), "observations");
		if (metadata.artifactPaths().transcriptText() != null) {
			Files.writeString(runDirectory.resolve("transcript.txt"), "transcript");
		}
	}

	private RunMetadata metadata(
			String callId,
			String scenarioId,
			String runMode,
			String status,
			String transcriptText
	) {
		Path runDirectory = tempDir.resolve("outputs").resolve(callId);
		return new RunMetadata(
				callId,
				scenarioId,
				runMode,
				"+18054398008",
				"retell".equals(runMode) ? "retell_call_123" : null,
				OffsetDateTime.parse("2026-05-27T10:00:00-04:00"),
				OffsetDateTime.parse("2026-05-27T10:05:00-04:00"),
				status,
				new ArtifactPaths(
						runDirectory.resolve("scenario.yaml").toString(),
						runDirectory.resolve("metadata.json").toString(),
						transcriptText == null ? null : runDirectory.resolve("transcript.txt").toString(),
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
