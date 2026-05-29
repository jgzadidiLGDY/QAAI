package com.qaai.runner;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qaai.artifacts.ArtifactCompleteness;
import com.qaai.artifacts.ArtifactCompletenessChecker;
import com.qaai.artifacts.ArtifactWriter;
import com.qaai.artifacts.NormalizedTranscript;
import com.qaai.config.QaaiProperties;
import com.qaai.scenario.PatientSimulationPromptBuilder;
import com.qaai.scenario.ScenarioLoader;
import com.qaai.scenario.ScenarioValidator;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TextChatRunnerTest {

	@TempDir
	private Path tempDir;

	@Test
	void runsScenarioAndWritesInspectableTextChatArtifacts() throws Exception {
		Path outputs = tempDir.resolve("outputs");
		TextChatRunner runner = new TextChatRunner(
				new ScenarioLoader(),
				new ScenarioValidator(),
				new PatientSimulationPromptBuilder(),
				new ArtifactWriter(new ObjectMapper(), outputs),
				new QaaiProperties(
						null,
						null,
						null,
						null,
						null,
						null,
						new QaaiProperties.Target("+18054398008"),
						new QaaiProperties.Outputs(outputs.toString())
				),
				Clock.fixed(Instant.parse("2026-05-29T16:15:00Z"), ZoneOffset.ofHours(-4)),
				() -> "chat1234"
		);

		ScenarioRunResult result = runner.run(Path.of("scenarios/appointment-reschedule.yaml"));

		assertThat(result.metadata().callId()).isEqualTo("call_20260529_121500_chat1234");
		assertThat(result.metadata().runMode()).isEqualTo("text_chat");
		assertThat(result.metadata().channel()).isEqualTo("text");
		assertThat(result.metadata().targetPhoneNumber()).isNull();
		assertThat(result.metadata().retellCallId()).isNull();
		assertThat(result.artifacts().scenarioSnapshot()).exists();
		assertThat(result.artifacts().metadata()).exists();
		assertThat(result.artifacts().transcriptText()).exists();
		assertThat(result.artifacts().patientSimulation()).exists();
		assertThat(result.artifacts().observationsMarkdown()).exists();
		assertThat(Path.of(result.metadata().artifactPaths().transcriptJson())).exists();
		assertThat(result.metadata().artifactPaths().audio()).isNull();
		assertThat(result.metadata().artifactPaths().manifest()).isNull();

		String transcriptText = Files.readString(result.artifacts().transcriptText());
		assertThat(transcriptText).contains(
				"Text Chat Transcript",
				"channel: text",
				"source: text_chat",
				"1. [patient] Hi, I need to reschedule my appointment.",
				"3. [patient] Next Tuesday or Wednesday morning would work for me."
		);

		NormalizedTranscript transcript = new ObjectMapper().findAndRegisterModules().readValue(
				Path.of(result.metadata().artifactPaths().transcriptJson()).toFile(),
				NormalizedTranscript.class
		);
		assertThat(transcript.source()).isEqualTo("text_chat");
		assertThat(transcript.turns()).hasSize(3);
		assertThat(transcript.turns().getFirst().speaker()).isEqualTo("patient");

		String observations = Files.readString(result.artifacts().observationsMarkdown());
		assertThat(observations).contains(
				"run_mode: text_chat",
				"channel: text",
				"Text chat transcript is available as normalized evidence for review."
		);

		ArtifactCompleteness completeness = new ArtifactCompletenessChecker().check(result.metadata());
		assertThat(completeness.complete()).isTrue();
		assertThat(completeness.missingRequiredArtifacts()).isEmpty();
		assertThat(completeness.warnings()).isEmpty();
	}
}
