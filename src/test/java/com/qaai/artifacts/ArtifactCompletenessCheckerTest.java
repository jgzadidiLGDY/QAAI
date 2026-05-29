package com.qaai.artifacts;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ArtifactCompletenessCheckerTest {

	@TempDir
	private Path tempDir;

	@Test
	void marksDryRunCompleteWhenExpectedArtifactsExist() throws Exception {
		Path runDirectory = tempDir.resolve("outputs").resolve("call_20260523_130000_test1234");
		Files.createDirectories(runDirectory);
		Files.writeString(runDirectory.resolve("scenario.yaml"), "id: appointment_reschedule_001%n".formatted());
		Files.writeString(runDirectory.resolve("metadata.json"), "{}");
		Files.writeString(runDirectory.resolve("transcript.txt"), "transcript");
		Files.writeString(runDirectory.resolve("patient_simulation.md"), "patient");
		Files.writeString(runDirectory.resolve("observations.md"), "observations");

		ArtifactCompleteness completeness = new ArtifactCompletenessChecker().check(metadata(
				"dry_run",
				"completed",
				runDirectory,
				null,
				null,
				null
		));

		assertThat(completeness.complete()).isTrue();
		assertThat(completeness.missingRequiredArtifacts()).isEmpty();
		assertThat(completeness.warnings()).isEmpty();
	}

	@Test
	void recordsMissingRequiredCapturedArtifactsAndOptionalAudioWarning() throws Exception {
		Path runDirectory = tempDir.resolve("outputs").resolve("call_20260523_130000_test1234");
		Files.createDirectories(runDirectory);
		Files.writeString(runDirectory.resolve("scenario.yaml"), "id: appointment_reschedule_001%n".formatted());
		Files.writeString(runDirectory.resolve("metadata.json"), "{}");
		Files.writeString(runDirectory.resolve("patient_simulation.md"), "patient");
		Files.writeString(runDirectory.resolve("observations.md"), "observations");

		ArtifactCompleteness completeness = new ArtifactCompletenessChecker().check(metadata(
				"retell",
				"artifacts_partially_captured",
				runDirectory,
				runDirectory.resolve("transcript.json").toString(),
				runDirectory.resolve("manifest.json").toString(),
				null
		));

		assertThat(completeness.complete()).isFalse();
		assertThat(completeness.missingRequiredArtifacts()).containsExactly(
				"transcript_text",
				"transcript_json",
				"manifest"
		);
		assertThat(completeness.warnings()).containsExactly("audio missing or unavailable");
	}

	@Test
	void requiresAnalysisArtifactsForAnalyzedRuns() throws Exception {
		Path runDirectory = tempDir.resolve("outputs").resolve("call_20260523_130000_test1234");
		Files.createDirectories(runDirectory);
		Files.writeString(runDirectory.resolve("scenario.yaml"), "id: appointment_reschedule_001%n".formatted());
		Files.writeString(runDirectory.resolve("metadata.json"), "{}");
		Files.writeString(runDirectory.resolve("transcript.txt"), "transcript");
		Files.writeString(runDirectory.resolve("transcript.json"), "{}");
		Files.writeString(runDirectory.resolve("patient_simulation.md"), "patient");
		Files.writeString(runDirectory.resolve("manifest.json"), "{}");
		Files.writeString(runDirectory.resolve("observations.md"), "observations");

		ArtifactCompleteness completeness = new ArtifactCompletenessChecker().check(metadata(
				"retell",
				"analysis_completed",
				runDirectory,
				runDirectory.resolve("transcript.json").toString(),
				runDirectory.resolve("manifest.json").toString(),
				null
		));

		assertThat(completeness.complete()).isFalse();
		assertThat(completeness.missingRequiredArtifacts()).containsExactly(
				"analysis_json",
				"analysis_markdown"
		);
	}

	@Test
	void textChatRunsRequireNormalizedTranscriptButNotManifestOrAudio() throws Exception {
		Path runDirectory = tempDir.resolve("outputs").resolve("call_20260529_121500_chat1234");
		Files.createDirectories(runDirectory);
		Files.writeString(runDirectory.resolve("scenario.yaml"), "id: appointment_reschedule_001%n".formatted());
		Files.writeString(runDirectory.resolve("metadata.json"), "{}");
		Files.writeString(runDirectory.resolve("transcript.txt"), "transcript");
		Files.writeString(runDirectory.resolve("transcript.json"), "{}");
		Files.writeString(runDirectory.resolve("patient_simulation.md"), "patient");
		Files.writeString(runDirectory.resolve("observations.md"), "observations");

		ArtifactCompleteness completeness = new ArtifactCompletenessChecker().check(new RunMetadata(
				"call_20260529_121500_chat1234",
				"appointment_reschedule_001",
				"text_chat",
				"text",
				null,
				null,
				OffsetDateTime.parse("2026-05-29T12:15:00-04:00"),
				OffsetDateTime.parse("2026-05-29T12:15:00-04:00"),
				"completed",
				new ArtifactPaths(
						runDirectory.resolve("scenario.yaml").toString(),
						runDirectory.resolve("metadata.json").toString(),
						runDirectory.resolve("transcript.txt").toString(),
						runDirectory.resolve("transcript.json").toString(),
						runDirectory.resolve("patient_simulation.md").toString(),
						null,
						null,
						null,
						null,
						runDirectory.resolve("observations.md").toString()
				)
		));

		assertThat(completeness.complete()).isTrue();
		assertThat(completeness.missingRequiredArtifacts()).isEmpty();
		assertThat(completeness.warnings()).isEmpty();
	}

	private RunMetadata metadata(
			String runMode,
			String status,
			Path runDirectory,
			String transcriptJson,
			String manifest,
			String audio
	) {
		return new RunMetadata(
				"call_20260523_130000_test1234",
				"appointment_reschedule_001",
				runMode,
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
						runDirectory.resolve("analysis.json").toString(),
						runDirectory.resolve("analysis.md").toString(),
						runDirectory.resolve("observations.md").toString()
				)
		);
	}
}
