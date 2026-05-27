package com.qaai.runner;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qaai.artifacts.ArtifactWriter;
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

class DryRunRunnerTest {

	@TempDir
	private Path tempDir;

	@Test
	void runsScenarioAndWritesDryRunArtifactBundle() throws Exception {
		QaaiProperties properties = new QaaiProperties(
				null,
				null,
				null,
				new QaaiProperties.Target("+18054398008"),
				new QaaiProperties.Outputs(tempDir.resolve("outputs").toString())
		);
		DryRunRunner runner = new DryRunRunner(
				new ScenarioLoader(),
				new ScenarioValidator(),
				new PatientSimulationPromptBuilder(),
				new ArtifactWriter(new ObjectMapper(), tempDir.resolve("outputs")),
				properties,
				Clock.fixed(Instant.parse("2026-05-23T17:00:00Z"), ZoneOffset.ofHours(-4)),
				() -> "test1234"
		);

		ScenarioRunResult result = runner.run(Path.of("scenarios/appointment-reschedule.yaml"));

		assertThat(result.metadata().callId()).isEqualTo("call_20260523_130000_test1234");
		assertThat(result.metadata().runMode()).isEqualTo("dry_run");
		assertThat(result.metadata().retellCallId()).isNull();
		assertThat(result.metadata().targetPhoneNumber()).isEqualTo("+18054398008");
		assertThat(result.artifacts().runDirectory()).exists();
		assertThat(result.artifacts().scenarioSnapshot()).exists();
		assertThat(result.artifacts().metadata()).exists();
		assertThat(result.artifacts().transcriptText()).exists();
		assertThat(result.artifacts().patientSimulation()).exists();
		assertThat(result.artifacts().observationsMarkdown()).exists();

		String transcript = Files.readString(result.artifacts().transcriptText());
		assertThat(transcript).contains(
				"source: dry_run",
				"Conversation Quality Guidance",
				"welcome_behavior: Start with the configured welcome message and clearly state the rescheduling need.",
				"Patient Turns",
				"1. [patient] Hi, I need to reschedule my appointment. (intent: greeting)",
				"3. [patient] Next Tuesday or Wednesday morning would work for me. (intent: availability)"
		);

		String observations = Files.readString(result.artifacts().observationsMarkdown());
		assertThat(observations).contains(
				"# Conversation Quality Observations",
				"call_id: call_20260523_130000_test1234",
				"## Before",
				"## After",
				"Agent may skip confirmation of the new appointment time."
		);

		String patientSimulation = Files.readString(result.artifacts().patientSimulation());
		assertThat(patientSimulation).contains(
				"# Patient Simulation Scenario",
				"Call reason: rescheduling my appointment",
				"Success condition: Agent confirms a new appointment date and time.",
				"Welcome behavior: Start with the configured welcome message",
				"Do not invent insurance details."
		);

		String metadata = Files.readString(result.artifacts().metadata());
		assertThat(metadata).contains(
				"\"run_mode\" : \"dry_run\"",
				"\"retell_call_id\" : null",
				"\"transcript_text\"",
				"\"patient_simulation\"",
				"\"observations_markdown\"",
				"\"reproducibility\" : {",
				"\"command\" : \"dry-run\"",
				"\"app_version\" : \"0.0.1-SNAPSHOT\""
		);
	}
}
