package com.qaai.runner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qaai.artifacts.ArtifactWriter;
import com.qaai.config.QaaiProperties;
import com.qaai.retell.RetellClient;
import com.qaai.retell.RetellOutboundCallRequest;
import com.qaai.retell.RetellOutboundCallResponse;
import com.qaai.scenario.PatientSimulationPromptBuilder;
import com.qaai.scenario.ScenarioLoader;
import com.qaai.scenario.ScenarioValidator;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.web.client.RestClient;

class RetellCallRunnerTest {

	@TempDir
	private Path tempDir;

	@Test
	void startsRetellCallAndWritesMetadataArtifactBundle() throws Exception {
		AtomicReference<RetellOutboundCallRequest> capturedRequest = new AtomicReference<>();
		QaaiProperties properties = properties(
				new QaaiProperties.Retell(
						"test-retell-key",
						"agent_123",
						"+15555550100",
						"https://api.example.test",
						Duration.ofSeconds(30),
						Duration.ofSeconds(60)
				)
		);
		RetellCallRunner runner = new RetellCallRunner(
				new ScenarioLoader(),
				new ScenarioValidator(),
				new PatientSimulationPromptBuilder(),
				new ArtifactWriter(new ObjectMapper(), tempDir.resolve("outputs")),
				retellClient(capturedRequest),
				properties,
				Clock.fixed(Instant.parse("2026-05-23T17:00:00Z"), ZoneOffset.ofHours(-4)),
				() -> "test1234"
		);

		ScenarioRunResult result = runner.run(Path.of("scenarios/appointment-reschedule.yaml"));

		assertThat(result.metadata().callId()).isEqualTo("call_20260523_130000_test1234");
		assertThat(result.metadata().runMode()).isEqualTo("retell");
		assertThat(result.metadata().retellCallId()).isEqualTo("retell_call_123");
		assertThat(result.metadata().status()).isEqualTo("retell_registered");
		assertThat(result.artifacts().scenarioSnapshot()).exists();
		assertThat(result.artifacts().metadata()).exists();
		assertThat(result.artifacts().transcriptText()).isNull();
		assertThat(result.artifacts().patientSimulation()).exists();
		assertThat(result.artifacts().observationsMarkdown()).exists();

		assertThat(capturedRequest.get().fromNumber()).isEqualTo("+15555550100");
		assertThat(capturedRequest.get().toNumber()).isEqualTo("+18054398008");
		assertThat(capturedRequest.get().overrideAgentId()).isEqualTo("agent_123");
		assertThat(capturedRequest.get().metadata()).containsEntry("qaai_call_id", "call_20260523_130000_test1234");
		assertThat(capturedRequest.get().metadata()).containsEntry("scenario_id", "appointment_reschedule_001");
		assertThat(capturedRequest.get().retellLlmDynamicVariables())
				.containsEntry("workflow", "appointment_rescheduling")
				.containsEntry("patient_name", "Maria Lopez")
				.containsEntry("call_reason", "rescheduling my appointment")
				.containsEntry("welcome_behavior",
						"Start with the configured welcome message and clearly state the rescheduling need.");
		assertThat(capturedRequest.get().retellLlmDynamicVariables().get("patient_simulation_prompt"))
				.contains(
						"# Patient Simulation Scenario",
						"Scenario ID: appointment_reschedule_001",
						"Must Not Provide Or Invent"
				);

		String metadata = Files.readString(result.artifacts().metadata());
		assertThat(metadata).contains(
				"\"run_mode\" : \"retell\"",
				"\"retell_call_id\" : \"retell_call_123\"",
				"\"status\" : \"retell_registered\"",
				"\"transcript_text\" : null",
				"\"patient_simulation\"",
				"\"observations_markdown\""
		);

		String observations = Files.readString(result.artifacts().observationsMarkdown());
		assertThat(observations).contains(
				"# Retell Call Start Observations",
				"retell_call_id: retell_call_123",
				"Transcript and recording capture remain out of scope"
		);
	}

	@Test
	void requiresRetellConfigForRealCallMode() {
		RetellCallRunner runner = new RetellCallRunner(
				new ScenarioLoader(),
				new ScenarioValidator(),
				new PatientSimulationPromptBuilder(),
				new ArtifactWriter(new ObjectMapper(), tempDir.resolve("outputs")),
				retellClient(new AtomicReference<>()),
				properties(new QaaiProperties.Retell("", "agent_123", "+15555550100", "https://api.example.test",
						Duration.ofSeconds(30), Duration.ofSeconds(60))),
				Clock.systemUTC(),
				() -> "test1234"
		);

		assertThatThrownBy(() -> runner.run(Path.of("scenarios/appointment-reschedule.yaml")))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("RETELL_API_KEY is required");
	}

	private QaaiProperties properties(QaaiProperties.Retell retell) {
		return new QaaiProperties(
				retell,
				null,
				new QaaiProperties.Target("+18054398008"),
				new QaaiProperties.Outputs(tempDir.resolve("outputs").toString())
		);
	}

	private RetellClient retellClient(AtomicReference<RetellOutboundCallRequest> capturedRequest) {
		return new RetellClient(RestClient.builder().build(), "unused") {
			@Override
			public RetellOutboundCallResponse createPhoneCall(RetellOutboundCallRequest request) {
				capturedRequest.set(request);
				return new RetellOutboundCallResponse(
						"retell_call_123",
						"registered",
						"agent_123",
						"+15555550100",
						"+18054398008",
						"outbound",
						Map.of("qaai_call_id", "call_20260523_130000_test1234")
				);
			}
		};
	}
}
