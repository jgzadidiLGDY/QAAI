package com.qaai.artifacts;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ArtifactWriterTest {

	@TempDir
	private Path tempDir;

	@Test
	void writesDryRunArtifacts() throws Exception {
		Path scenarioPath = tempDir.resolve("source-scenario.yaml");
		Files.writeString(scenarioPath, "id: appointment_reschedule_001%n".formatted());
		Path outputs = tempDir.resolve("outputs");
		Path runDirectory = outputs.resolve("call_20260523_130000_test1234");
		ArtifactWriter writer = new ArtifactWriter(new ObjectMapper(), outputs);
		RunMetadata metadata = new RunMetadata(
				"call_20260523_130000_test1234",
				"appointment_reschedule_001",
				"dry_run",
				"+18054398008",
				null,
				OffsetDateTime.parse("2026-05-23T13:00:00-04:00"),
				OffsetDateTime.parse("2026-05-23T13:00:01-04:00"),
				"completed",
				new ArtifactPaths(
						runDirectory.resolve("scenario.yaml").toString(),
						runDirectory.resolve("metadata.json").toString(),
						runDirectory.resolve("transcript.txt").toString(),
						null,
						runDirectory.resolve("patient_simulation.md").toString(),
						null,
						null,
						null,
						null,
						runDirectory.resolve("observations.md").toString()
				)
		);

		ArtifactBundle bundle = writer.writeDryRunArtifacts(
				"call_20260523_130000_test1234",
				scenarioPath,
				metadata,
				"# Patient Simulation Scenario%n".formatted(),
				"Dry Run Transcript%nsource: dry_run%n".formatted(),
				"# Conversation Quality Observations%n".formatted()
		);

		assertThat(bundle.runDirectory()).exists();
		assertThat(bundle.scenarioSnapshot()).hasContent("id: appointment_reschedule_001%n".formatted());
		assertThat(bundle.patientSimulation()).hasContent("# Patient Simulation Scenario%n".formatted());
		assertThat(bundle.transcriptText()).hasContent("Dry Run Transcript%nsource: dry_run%n".formatted());
		assertThat(bundle.observationsMarkdown()).hasContent("# Conversation Quality Observations%n".formatted());
		assertThat(bundle.metadata()).content()
				.contains(
						"\"call_id\" : \"call_20260523_130000_test1234\"",
						"\"scenario_id\" : \"appointment_reschedule_001\"",
						"\"run_mode\" : \"dry_run\"",
						"\"status\" : \"completed\""
				);
		assertThat(outputs.resolve("index.jsonl")).exists();
		assertThat(Files.readString(outputs.resolve("index.jsonl"))).contains(
				"\"call_id\":\"call_20260523_130000_test1234\"",
				"\"complete\":true"
		);
	}
}
