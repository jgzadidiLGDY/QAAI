package com.qaai.runner;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qaai.artifacts.ArtifactCompletenessChecker;
import com.qaai.artifacts.ArtifactPaths;
import com.qaai.artifacts.RunMetadata;
import com.qaai.artifacts.RunIndexWriter;
import com.qaai.reporting.ReportGenerationService;
import com.qaai.reporting.ReportResult;
import com.qaai.review.MultiLensReviewException;
import com.qaai.review.MultiLensReviewResult;
import com.qaai.review.MultiLensReviewService;
import com.qaai.scenariogeneration.ScenarioGenerationResult;
import com.qaai.scenariogeneration.ScenarioGenerationService;
import com.qaai.reporting.StaticReportRenderer;
import com.qaai.scenario.ScenarioLoader;
import com.qaai.scenario.ScenarioValidator;
import com.qaai.suite.SuiteRunResult;
import com.qaai.suite.SuiteRunService;
import java.nio.file.Path;
import java.time.Clock;
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
		ScenarioRunnerCommand command = new ScenarioRunnerCommand(
				null, null, null, null, null, null, null, null, null, null, null, null);

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
				null,
				artifactCaptureService,
				null,
				null,
				null,
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
		ScenarioRunnerCommand command = new ScenarioRunnerCommand(
				null, null, null, null, null, null, null, null, null, null, null, null);

		command.run(new DefaultApplicationArguments());

		assertThat(command.getExitCode()).isZero();
		assertThat(output).contains("--scenario=<path> [--run-mode=dry-run|text-chat|retell]");
		assertThat(output).contains("--suite=<path>");
		assertThat(output).contains("--show-run --call-id=<local_call_id>");
		assertThat(output).contains("--list-runs [--scenario=<scenario_id>]");
		assertThat(output).contains("--evaluate-call --call-id=<local_call_id>");
		assertThat(output).contains("--multi-lens-review --call-id=<local_call_id>");
		assertThat(output).contains("--generate-report");
		assertThat(output).contains("--generate-scenarios --agent-description=<description>");
	}

	@Test
	void routesSuiteCommandToSuiteRunService(CapturedOutput output) {
		SuiteRunService suiteRunService = new SuiteRunService(
				null, null, null, null, null, null, new ObjectMapper(),
				Path.of("outputs"), Path.of("agent-profiles"), Clock.systemDefaultZone(), () -> "ignored"
		) {
			@Override
			public SuiteRunResult run(Path suitePath) {
				Path suiteDirectory = Path.of("outputs", "suites", "suite_20260530_120000_suite123");
				return new SuiteRunResult(
						"suite_20260530_120000_suite123",
						suiteDirectory,
						suiteDirectory.resolve("suite.yaml"),
						suiteDirectory.resolve("agent-profile.yaml"),
						suiteDirectory.resolve("suite-report.json"),
						suiteDirectory.resolve("suite-report.md")
				);
			}
		};
		ScenarioRunnerCommand command = new ScenarioRunnerCommand(
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				suiteRunService,
				null,
				null,
				null
		);

		command.run(new DefaultApplicationArguments("--suite=suites/receptionist-smoke.yaml"));

		assertThat(command.getExitCode()).isZero();
		assertThat(output).contains("Suite run completed");
		assertThat(output).contains("suite_run_id: suite_20260530_120000_suite123");
		assertThat(output).contains("suite_report_json: outputs\\suites\\suite_20260530_120000_suite123\\suite-report.json");
	}

	@Test
	void runModeWithoutScenarioReportsScenarioRequirement(CapturedOutput output) {
		ScenarioRunnerCommand command = new ScenarioRunnerCommand(
				null, null, null, null, null, null, null, null, null, null, null, null);

		command.run(new DefaultApplicationArguments("--run-mode=text-chat"));

		assertThat(command.getExitCode()).isEqualTo(1);
		assertThat(output).contains(
				"Error: command=scenario: "
						+ "Provide a scenario path with --scenario=scenarios/appointment-reschedule.yaml when using --run-mode",
				"Provide a scenario path with --scenario=scenarios/appointment-reschedule.yaml when using --run-mode"
		);
	}

	@Test
	void routesTextChatRunModeToTextChatRunner(CapturedOutput output) {
		TextChatRunner textChatRunner = new TextChatRunner(null, null, null, null, null) {
			@Override
			public ScenarioRunResult run(Path scenarioPath) {
				Path runDirectory = Path.of("outputs", "call_20260529_121500_chat1234");
				RunMetadata metadata = new RunMetadata(
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
				);
				return new ScenarioRunResult(
						metadata,
						new com.qaai.artifacts.ArtifactBundle(
								metadata.callId(),
								runDirectory,
								runDirectory.resolve("scenario.yaml"),
								runDirectory.resolve("metadata.json"),
								runDirectory.resolve("transcript.txt"),
								runDirectory.resolve("patient_simulation.md"),
								runDirectory.resolve("observations.md")
						)
				);
			}
		};
		ScenarioRunnerCommand command = new ScenarioRunnerCommand(
				null,
				textChatRunner,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null
		);

		command.run(new DefaultApplicationArguments(
				"--scenario=scenarios/appointment-reschedule.yaml",
				"--run-mode=text-chat"
		));

		assertThat(command.getExitCode()).isZero();
		assertThat(output).contains("Text chat run completed");
		assertThat(output).contains("call_id: call_20260529_121500_chat1234");
	}

	@Test
	void multiLensReviewPrintsReviewArtifacts(CapturedOutput output) {
		String callId = "call_20260528_120000_review123";
		MultiLensReviewService reviewService = new MultiLensReviewService(null, null, null) {
			@Override
			public MultiLensReviewResult review(String requestedCallId) {
				Path runDirectory = Path.of("outputs").resolve(requestedCallId);
				RunMetadata metadata = new RunMetadata(
						requestedCallId,
						"appointment_reschedule_001",
						"retell",
						"+18054398008",
						"retell_call_123",
						OffsetDateTime.parse("2026-05-28T12:00:00-04:00"),
						OffsetDateTime.parse("2026-05-28T12:02:00-04:00"),
						"multi_lens_review_completed",
						new ArtifactPaths(
								runDirectory.resolve("scenario.yaml").toString(),
								runDirectory.resolve("metadata.json").toString(),
								runDirectory.resolve("transcript.txt").toString(),
								runDirectory.resolve("transcript.json").toString(),
								runDirectory.resolve("patient_simulation.md").toString(),
								null,
								runDirectory.resolve("manifest.json").toString(),
								null,
								null,
								null,
								null,
								runDirectory.resolve("multi-lens-review.json").toString(),
								runDirectory.resolve("multi-lens-review.md").toString(),
								runDirectory.resolve("observations.md").toString()
						)
				);
				return new MultiLensReviewResult(
						metadata,
						runDirectory,
						runDirectory.resolve("multi-lens-review.json"),
						runDirectory.resolve("multi-lens-review.md")
				);
			}
		};
		ScenarioRunnerCommand command = new ScenarioRunnerCommand(
				null,
				null,
				null,
				null,
				null,
				null,
				reviewService,
				null,
				null,
				null,
				null,
				null
		);

		command.run(new DefaultApplicationArguments("--multi-lens-review", "--call-id=" + callId));

		assertThat(command.getExitCode()).isZero();
		assertThat(output).contains("Multi-lens review completed");
		assertThat(output).contains("multi_lens_review_json: outputs\\" + callId + "\\multi-lens-review.json");
	}

	@Test
	void multiLensReviewReportsDisabledProvider(CapturedOutput output) {
		String callId = "call_20260528_120000_review123";
		MultiLensReviewService reviewService = new MultiLensReviewService(null, null, null) {
			@Override
			public MultiLensReviewResult review(String requestedCallId) {
				throw new MultiLensReviewException(
						"Multi-lens review is disabled by QAAI_REVIEW_PROVIDER=disabled"
				);
			}
		};
		ScenarioRunnerCommand command = new ScenarioRunnerCommand(
				null,
				null,
				null,
				null,
				null,
				null,
				reviewService,
				null,
				null,
				null,
				null,
				null
		);

		command.run(new DefaultApplicationArguments("--multi-lens-review", "--call-id=" + callId));

		assertThat(command.getExitCode()).isEqualTo(1);
		assertThat(output).contains("Multi-lens review is disabled");
	}

	@Test
	void generateReportPrintsReportArtifacts(CapturedOutput output) {
		ReportGenerationService reportGenerationService = new ReportGenerationService(
				new ObjectMapper(),
				Path.of("outputs"),
				Path.of("scenarios"),
				new RunIndexWriter(new ObjectMapper(), Path.of("outputs"), new ArtifactCompletenessChecker()),
				new ArtifactCompletenessChecker(),
				new ScenarioLoader(),
				new StaticReportRenderer(),
				Clock.systemDefaultZone()
		) {
			@Override
			public ReportResult generate() {
				Path reportDirectory = Path.of("outputs", "reports", "report_20260528_120000");
				return new ReportResult(
						"report_20260528_120000",
						reportDirectory,
						reportDirectory.resolve("report.json"),
						reportDirectory.resolve("report.md"),
						reportDirectory.resolve("index.html")
				);
			}
		};
		ScenarioRunnerCommand command = new ScenarioRunnerCommand(
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				reportGenerationService,
				null,
				null,
				null,
				null
		);

		command.run(new DefaultApplicationArguments("--generate-report"));

		assertThat(command.getExitCode()).isZero();
		assertThat(output).contains("Report generated");
		assertThat(output).contains("report_id: report_20260528_120000");
		assertThat(output).contains("report_html: outputs\\reports\\report_20260528_120000\\index.html");
	}

	@Test
	void generateScenariosPrintsReviewArtifactPaths(CapturedOutput output) {
		ScenarioGenerationService scenarioGenerationService = new ScenarioGenerationService(
				Path.of("outputs"),
				new ObjectMapper(),
				new com.fasterxml.jackson.databind.ObjectMapper(new com.fasterxml.jackson.dataformat.yaml.YAMLFactory()),
				null,
				null,
				null,
				Clock.systemDefaultZone()
		) {
			@Override
			public ScenarioGenerationResult generate(String agentDescription, Integer requestedScenarioCount) {
				Path generationDirectory = Path.of("outputs", "scenario-generation", "scenario_generation_20260528_120000");
				return new ScenarioGenerationResult(
						"scenario_generation_20260528_120000",
						generationDirectory,
						generationDirectory.resolve("agent-description.md"),
						generationDirectory.resolve("coverage-plan.md"),
						generationDirectory.resolve("generation-report.json"),
						generationDirectory.resolve("generation-report.md")
				);
			}
		};
		ScenarioRunnerCommand command = new ScenarioRunnerCommand(
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				scenarioGenerationService,
				null,
				null,
				null
		);

		command.run(new DefaultApplicationArguments(
				"--generate-scenarios",
				"--agent-description=medical office scheduling agent",
				"--scenario-count=2"
		));

		assertThat(command.getExitCode()).isZero();
		assertThat(output).contains("Scenario drafts generated");
		assertThat(output).contains("generation_id: scenario_generation_20260528_120000");
		assertThat(output).contains("review_required: true");
	}

	@Test
	void generateScenariosRequiresAgentDescription() {
		ScenarioRunnerCommand command = new ScenarioRunnerCommand(
				null, null, null, null, null, null, null, null, null, null, null, null);

		command.run(new DefaultApplicationArguments("--generate-scenarios"));

		assertThat(command.getExitCode()).isEqualTo(1);
	}

	@Test
	void generateScenariosReportsDisabledProvider(CapturedOutput output) {
		ScenarioGenerationService scenarioGenerationService = new ScenarioGenerationService(
				Path.of("outputs"),
				new ObjectMapper(),
				new com.fasterxml.jackson.databind.ObjectMapper(new com.fasterxml.jackson.dataformat.yaml.YAMLFactory()),
				new com.qaai.scenariogeneration.ScenarioGenerationPromptBuilder(),
				request -> {
					throw new com.qaai.scenariogeneration.ScenarioGenerationException(
							"Scenario generation is disabled"
					);
				},
				new ScenarioValidator(),
				Clock.systemDefaultZone()
		);
		ScenarioRunnerCommand command = new ScenarioRunnerCommand(
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				scenarioGenerationService,
				null,
				null,
				null
		);

		command.run(new DefaultApplicationArguments(
				"--generate-scenarios",
				"--agent-description=medical-office-scheduling-agent"
		));

		assertThat(command.getExitCode()).isEqualTo(1);
		assertThat(output).contains("Scenario generation is disabled");
	}
}
