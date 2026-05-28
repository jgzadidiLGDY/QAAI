package com.qaai.runner;

import static org.assertj.core.api.Assertions.assertThat;

import com.qaai.artifacts.ArtifactPaths;
import com.qaai.artifacts.RunMetadata;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

@ExtendWith(OutputCaptureExtension.class)
class ScenarioRunnerCommandTest {

	@Test
	void reviewConversationRequiresCallId() {
		ScenarioRunnerCommand command = new ScenarioRunnerCommand(null, null, null, null, null, null, null, null);

		command.run(new DefaultApplicationArguments("--review-conversation"));

		assertThat(command.getExitCode()).isEqualTo(1);
	}

	@Test
	void captureArtifactsPrintsConversationReviewNextStep(CapturedOutput output) {
		String callId = "call_20260527_135012_1e796914";
		ArtifactCaptureService artifactCaptureService = new ArtifactCaptureService(null, null) {
			@Override
			public ArtifactCaptureResult capture(String requestedCallId) {
				Path runDirectory = Path.of("outputs").resolve(requestedCallId);
				RunMetadata metadata = new RunMetadata(
						requestedCallId,
						"appointment_reschedule_001",
						"retell",
						"+18054398008",
						"retell_call_123",
						OffsetDateTime.parse("2026-05-27T13:50:12-04:00"),
						OffsetDateTime.parse("2026-05-27T13:51:04-04:00"),
						52L,
						"artifacts_captured",
						new ArtifactPaths(
								runDirectory.resolve("scenario.yaml").toString(),
								runDirectory.resolve("metadata.json").toString(),
								runDirectory.resolve("transcript.txt").toString(),
								runDirectory.resolve("transcript.json").toString(),
								runDirectory.resolve("patient_simulation.md").toString(),
								runDirectory.resolve("audio.wav").toString(),
								runDirectory.resolve("manifest.json").toString(),
								null,
								null,
								runDirectory.resolve("observations.md").toString()
						),
						null,
						null
				);
				return new ArtifactCaptureResult(
						metadata,
						runDirectory,
						runDirectory.resolve("transcript.json"),
						runDirectory.resolve("transcript.txt"),
						runDirectory.resolve("audio.wav"),
						runDirectory.resolve("manifest.json")
				);
			}
		};
		ScenarioRunnerCommand command = new ScenarioRunnerCommand(
				null,
				null,
				artifactCaptureService,
				null,
				null,
				null,
				null,
				null
		);

		command.run(new DefaultApplicationArguments("--capture-artifacts", "--call-id=" + callId));

		assertThat(command.getExitCode()).isZero();
		assertThat(output).contains("next_step: --review-conversation --call-id=" + callId);
	}

	@Test
	void printsHelpWhenNoCommandIsProvided(CapturedOutput output) {
		ScenarioRunnerCommand command = new ScenarioRunnerCommand(null, null, null, null, null, null, null, null);

		command.run(new DefaultApplicationArguments());

		assertThat(command.getExitCode()).isZero();
		assertThat(output).contains("--show-run --call-id=<local_call_id>");
		assertThat(output).contains("--list-runs [--scenario=<scenario_id>]");
		assertThat(output).contains("--evaluate-call --call-id=<local_call_id>");
	}
}
