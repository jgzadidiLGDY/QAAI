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
		ArtifactWriter writer = new ArtifactWriter(new ObjectMapper(), tempDir.resolve("outputs"));
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
						"outputs/call_20260523_130000_test1234/scenario.yaml",
						"outputs/call_20260523_130000_test1234/metadata.json",
						"outputs/call_20260523_130000_test1234/transcript.txt",
						null,
						null,
						null,
						"outputs/call_20260523_130000_test1234/observations.md"
				)
		);

		ArtifactBundle bundle = writer.writeDryRunArtifacts(
				"call_20260523_130000_test1234",
				scenarioPath,
				metadata,
				"Dry Run Transcript%nsource: dry_run%n".formatted(),
				"# Conversation Quality Observations%n".formatted()
		);

		assertThat(bundle.runDirectory()).exists();
		assertThat(bundle.scenarioSnapshot()).hasContent("id: appointment_reschedule_001%n".formatted());
		assertThat(bundle.transcriptText()).hasContent("Dry Run Transcript%nsource: dry_run%n".formatted());
		assertThat(bundle.observationsMarkdown()).hasContent("# Conversation Quality Observations%n".formatted());
		assertThat(bundle.metadata()).content()
				.contains(
						"\"call_id\" : \"call_20260523_130000_test1234\"",
						"\"scenario_id\" : \"appointment_reschedule_001\"",
						"\"run_mode\" : \"dry_run\"",
						"\"status\" : \"completed\""
				);
	}
}
