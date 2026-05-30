package com.qaai.suite;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qaai.agent.AgentProfileLoader;
import com.qaai.agent.AgentProfileValidator;
import com.qaai.artifacts.ArtifactCompletenessChecker;
import com.qaai.artifacts.ArtifactWriter;
import com.qaai.artifacts.RunMetadata;
import com.qaai.config.QaaiProperties;
import com.qaai.runner.TextChatRunner;
import com.qaai.scenario.PatientSimulationPromptBuilder;
import com.qaai.scenario.ScenarioLoader;
import com.qaai.scenario.ScenarioValidator;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SuiteRunServiceTest {

	@TempDir
	private Path tempDir;

	@Test
	void runsSuiteAndLinksAgentAndSuiteMetadata() throws Exception {
		Path outputs = tempDir.resolve("outputs");
		Clock clock = Clock.fixed(Instant.parse("2026-05-30T16:00:00Z"), ZoneOffset.ofHours(-4));
		AtomicInteger callIds = new AtomicInteger(1);
		TextChatRunner textChatRunner = new TextChatRunner(
				new ScenarioLoader(),
				new ScenarioValidator(),
				new PatientSimulationPromptBuilder(),
				new ArtifactWriter(new ObjectMapper(), outputs),
				new QaaiProperties(null, null, null, null, null, null, null,
						new QaaiProperties.Outputs(outputs.toString())),
				clock,
				() -> "chat000" + callIds.getAndIncrement()
		);
		SuiteRunService service = new SuiteRunService(
				new ScenarioSuiteLoader(),
				new ScenarioSuiteValidator(new ScenarioLoader(), new ScenarioValidator()),
				new AgentProfileLoader(),
				new AgentProfileValidator(),
				textChatRunner,
				new ArtifactCompletenessChecker(),
				new ObjectMapper(),
				outputs,
				Path.of("agent-profiles").toAbsolutePath(),
				clock,
				() -> "suite123"
		);

		SuiteRunResult result = service.run(Path.of("suites/receptionist-smoke.yaml").toAbsolutePath());

		assertThat(result.suiteRunId()).isEqualTo("suite_20260530_120000_suite123");
		assertThat(result.suiteSnapshot()).exists();
		assertThat(result.agentProfileSnapshot()).exists();
		assertThat(result.suiteReportJson()).exists();
		assertThat(result.suiteReportMarkdown()).exists();

		SuiteRunReport report = new ObjectMapper().findAndRegisterModules().readValue(
				result.suiteReportJson().toFile(),
				SuiteRunReport.class
		);
		assertThat(report.suiteId()).isEqualTo("receptionist_smoke_suite");
		assertThat(report.agentProfileId()).isEqualTo("medical_receptionist_demo");
		assertThat(report.runMode()).isEqualTo("text-chat");
		assertThat(report.humanReviewRequired()).isTrue();
		assertThat(report.runs()).hasSize(3);

		RunMetadata firstMetadata = new ObjectMapper().findAndRegisterModules().readValue(
				Path.of(report.runs().getFirst().metadataPath()).toFile(),
				RunMetadata.class
		);
		assertThat(firstMetadata.agentProfileId()).isEqualTo("medical_receptionist_demo");
		assertThat(firstMetadata.suiteId()).isEqualTo("receptionist_smoke_suite");
		assertThat(firstMetadata.suiteRunId()).isEqualTo("suite_20260530_120000_suite123");
		assertThat(firstMetadata.runMode()).isEqualTo("text_chat");
		assertThat(Path.of(firstMetadata.artifactPaths().transcriptJson())).exists();

		String suiteMarkdown = Files.readString(result.suiteReportMarkdown());
		assertThat(suiteMarkdown).contains(
				"suite_run_id: suite_20260530_120000_suite123",
				"agent_profile_id: medical_receptionist_demo",
				"Suite reports summarize artifacts for human review"
		);
	}
}
