package com.qaai.artifacts;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RunIndexWriterTest {

	@TempDir
	private Path tempDir;

	@Test
	void appendsJsonlEntriesForRunLifecycleUpdates() throws Exception {
		Path outputs = tempDir.resolve("outputs");
		Path runDirectory = outputs.resolve("call_20260523_130000_test1234");
		Files.createDirectories(runDirectory);
		Files.writeString(runDirectory.resolve("scenario.yaml"), "id: appointment_reschedule_001%n".formatted());
		Files.writeString(runDirectory.resolve("metadata.json"), "{}");
		Files.writeString(runDirectory.resolve("patient_simulation.md"), "patient");
		Files.writeString(runDirectory.resolve("observations.md"), "observations");

		RunIndexWriter writer = new RunIndexWriter(
				new ObjectMapper(),
				outputs,
				new ArtifactCompletenessChecker()
		);

		writer.append(metadata("retell_registered", runDirectory, null, null, null, null, null));
		Files.writeString(runDirectory.resolve("transcript.txt"), "transcript");
		Files.writeString(runDirectory.resolve("transcript.json"), "{}");
		Files.writeString(runDirectory.resolve("manifest.json"), "{}");
		writer.append(metadata(
				"artifacts_partially_captured",
				runDirectory,
				runDirectory.resolve("transcript.json").toString(),
				runDirectory.resolve("manifest.json").toString(),
				null,
				null,
				null
		));

		assertThat(writer.indexPath()).exists();
		assertThat(Files.readAllLines(writer.indexPath())).hasSize(2);
		assertThat(writer.readAll())
				.extracting(RunIndexEntry::status)
				.containsExactly("retell_registered", "artifacts_partially_captured");
		assertThat(writer.readAll().get(0).complete()).isTrue();
		assertThat(writer.readAll().get(1).complete()).isTrue();
		assertThat(writer.readAll().get(1).warnings()).containsExactly("audio missing or unavailable");
	}

	@Test
	void readsEmptyIndexWhenNoRunsHaveBeenRecorded() {
		RunIndexWriter writer = new RunIndexWriter(
				new ObjectMapper(),
				tempDir.resolve("outputs"),
				new ArtifactCompletenessChecker()
		);

		assertThat(writer.readAll()).isEmpty();
	}

	private RunMetadata metadata(
			String status,
			Path runDirectory,
			String transcriptJson,
			String manifest,
			String analysisJson,
			String analysisMarkdown,
			String audio
	) {
		return new RunMetadata(
				"call_20260523_130000_test1234",
				"appointment_reschedule_001",
				"retell",
				"+18054398008",
				"retell_call_123",
				OffsetDateTime.parse("2026-05-23T13:00:00-04:00"),
				OffsetDateTime.parse("2026-05-23T13:10:00-04:00"),
				status,
				new ArtifactPaths(
						runDirectory.resolve("scenario.yaml").toString(),
						runDirectory.resolve("metadata.json").toString(),
						runDirectory.resolve("transcript.txt").toString(),
						transcriptJson,
						runDirectory.resolve("patient_simulation.md").toString(),
						audio,
						manifest,
						analysisJson,
						analysisMarkdown,
						runDirectory.resolve("observations.md").toString()
				)
		);
	}
}
