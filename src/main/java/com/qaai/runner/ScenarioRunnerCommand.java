package com.qaai.runner;

import com.qaai.analysis.AnalysisResult;
import com.qaai.analysis.AnalysisService;
import com.qaai.artifacts.ArtifactCompleteness;
import com.qaai.artifacts.ArtifactCompleteness.ArtifactStatus;
import com.qaai.artifacts.ArtifactPaths;
import com.qaai.artifacts.RunIndexEntry;
import com.qaai.artifacts.RunIndexWriter;
import com.qaai.artifacts.RunMetadata;
import com.qaai.evaluation.EvaluationResult;
import com.qaai.evaluation.EvaluationService;
import com.qaai.quality.ConversationQualityReviewResult;
import com.qaai.quality.ConversationQualityReviewService;
import com.qaai.reporting.ReportGenerationService;
import com.qaai.reporting.ReportResult;
import com.qaai.review.MultiLensReviewResult;
import com.qaai.review.MultiLensReviewService;
import com.qaai.scenariogeneration.ScenarioGenerationResult;
import com.qaai.scenariogeneration.ScenarioGenerationService;
import java.nio.file.Path;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.stereotype.Component;

@Component
public class ScenarioRunnerCommand implements ApplicationRunner, ExitCodeGenerator {

	private static final Logger LOGGER = LoggerFactory.getLogger(ScenarioRunnerCommand.class);

	private final DryRunRunner dryRunRunner;
	private final RetellCallRunner retellCallRunner;
	private final ArtifactCaptureService artifactCaptureService;
	private final AnalysisService analysisService;
	private final EvaluationService evaluationService;
	private final MultiLensReviewService multiLensReviewService;
	private final ReportGenerationService reportGenerationService;
	private final ScenarioGenerationService scenarioGenerationService;
	private final RunIndexWriter runIndexWriter;
	private final ConversationQualityReviewService conversationQualityReviewService;
	private final RunInspectionService runInspectionService;
	private int exitCode;

	public ScenarioRunnerCommand(
			DryRunRunner dryRunRunner,
			RetellCallRunner retellCallRunner,
			ArtifactCaptureService artifactCaptureService,
			AnalysisService analysisService,
			EvaluationService evaluationService,
			MultiLensReviewService multiLensReviewService,
			ReportGenerationService reportGenerationService,
			ScenarioGenerationService scenarioGenerationService,
			RunIndexWriter runIndexWriter,
			ConversationQualityReviewService conversationQualityReviewService,
			RunInspectionService runInspectionService
	) {
		this.dryRunRunner = dryRunRunner;
		this.retellCallRunner = retellCallRunner;
		this.artifactCaptureService = artifactCaptureService;
		this.analysisService = analysisService;
		this.evaluationService = evaluationService;
		this.multiLensReviewService = multiLensReviewService;
		this.reportGenerationService = reportGenerationService;
		this.scenarioGenerationService = scenarioGenerationService;
		this.runIndexWriter = runIndexWriter;
		this.conversationQualityReviewService = conversationQualityReviewService;
		this.runInspectionService = runInspectionService;
	}

	@Override
	public void run(ApplicationArguments args) {
		try {
			runCommand(args);
		} catch (RuntimeException exception) {
			exitCode = 1;
			LOGGER.warn("Command failed: {}", failureContext(args), exception);
			System.err.println("Error: " + failureContext(args) + ": " + exception.getMessage());
		}
	}

	@Override
	public int getExitCode() {
		return exitCode;
	}

	private void runCommand(ApplicationArguments args) {
		if (args.containsOption("list-runs")) {
			listRuns(args);
			return;
		}

		if (args.containsOption("show-run")) {
			showRun(callId(args, "--show-run"));
			return;
		}

		if (args.containsOption("analyze-call")) {
			AnalysisResult result = analysisService.analyze(callId(args, "--analyze-call"));
			System.out.println("Analysis completed");
			System.out.println("call_id: " + result.metadata().callId());
			System.out.println("status: " + result.metadata().status());
			System.out.println("analysis_json: " + result.analysisJson());
			System.out.println("analysis_markdown: " + result.analysisMarkdown());
			return;
		}

		if (args.containsOption("evaluate-call")) {
			EvaluationResult result = evaluationService.evaluate(callId(args, "--evaluate-call"));
			System.out.println("Evaluation completed");
			System.out.println("call_id: " + result.metadata().callId());
			System.out.println("status: " + result.metadata().status());
			System.out.println("evaluation_json: " + result.evaluationJson());
			System.out.println("evaluation_markdown: " + result.evaluationMarkdown());
			return;
		}

		if (args.containsOption("multi-lens-review")) {
			MultiLensReviewResult result = multiLensReviewService.review(callId(args, "--multi-lens-review"));
			System.out.println("Multi-lens review completed");
			System.out.println("call_id: " + result.metadata().callId());
			System.out.println("status: " + result.metadata().status());
			System.out.println("multi_lens_review_json: " + result.reviewJson());
			System.out.println("multi_lens_review_markdown: " + result.reviewMarkdown());
			return;
		}

		if (args.containsOption("generate-report")) {
			ReportResult result = reportGenerationService.generate();
			System.out.println("Report generated");
			System.out.println("report_id: " + result.reportId());
			System.out.println("report_directory: " + result.reportDirectory());
			System.out.println("report_json: " + result.reportJson());
			System.out.println("report_markdown: " + result.reportMarkdown());
			System.out.println("report_html: " + result.reportHtml());
			return;
		}

		if (args.containsOption("generate-scenarios")) {
			ScenarioGenerationResult result = scenarioGenerationService.generate(
					agentDescription(args),
					scenarioCount(args)
			);
			System.out.println("Scenario drafts generated");
			System.out.println("generation_id: " + result.generationId());
			System.out.println("generation_directory: " + result.generationDirectory());
			System.out.println("agent_description: " + result.agentDescription());
			System.out.println("coverage_plan: " + result.coveragePlan());
			System.out.println("generation_report_json: " + result.generationReportJson());
			System.out.println("generation_report_markdown: " + result.generationReportMarkdown());
			System.out.println("review_required: true");
			return;
		}

		if (args.containsOption("review-conversation")) {
			ConversationQualityReviewResult result = conversationQualityReviewService.review(
					callId(args, "--review-conversation")
			);
			System.out.println("Conversation-quality review completed");
			System.out.println("call_id: " + result.metadata().callId());
			System.out.println("status: " + result.metadata().status());
			System.out.println("observations_markdown: " + result.observationsMarkdown());
			return;
		}

		if (args.containsOption("capture-artifacts")) {
			ArtifactCaptureResult result = artifactCaptureService.capture(callId(args, "--capture-artifacts"));
			System.out.println("Artifact capture completed");
			System.out.println("call_id: " + result.metadata().callId());
			System.out.println("retell_call_id: " + result.metadata().retellCallId());
			System.out.println("status: " + result.metadata().status());
			System.out.println("artifacts: " + result.runDirectory());
			System.out.println("next_step: --review-conversation --call-id=" + result.metadata().callId());
			return;
		}

		if (!args.containsOption("scenario")) {
			printHelp();
			return;
		}

		List<String> scenarioValues = args.getOptionValues("scenario");
		if (scenarioValues == null || scenarioValues.isEmpty() || scenarioValues.getFirst().isBlank()) {
			throw new IllegalArgumentException(
					"Provide a scenario path with --scenario=scenarios/appointment-reschedule.yaml"
			);
		}

		String scenarioPath = scenarioValues.getFirst();
		String runMode = runMode(args);
		ScenarioRunResult result = runScenario(runMode, Path.of(scenarioPath));
		System.out.println(runModeLabel(runMode) + " completed");
		System.out.println("call_id: " + result.metadata().callId());
		if (result.metadata().retellCallId() != null) {
			System.out.println("retell_call_id: " + result.metadata().retellCallId());
		}
		System.out.println("artifacts: " + result.artifacts().runDirectory());
	}

	private ScenarioRunResult runScenario(String runMode, Path scenarioPath) {
		return switch (runMode) {
			case "dry-run" -> dryRunRunner.run(scenarioPath);
			case "retell" -> retellCallRunner.run(scenarioPath);
			default -> throw new IllegalArgumentException("Unsupported --run-mode=" + runMode
					+ ". Use dry-run or retell.");
		};
	}

	private String runMode(ApplicationArguments args) {
		if (!args.containsOption("run-mode")) {
			return "dry-run";
		}

		List<String> runModeValues = args.getOptionValues("run-mode");
		if (runModeValues == null || runModeValues.isEmpty() || runModeValues.getFirst().isBlank()) {
			throw new IllegalArgumentException("Provide --run-mode=dry-run or --run-mode=retell");
		}
		return runModeValues.getFirst();
	}

	private String runModeLabel(String runMode) {
		if ("dry-run".equals(runMode)) {
			return "Dry run";
		}
		return "Retell call start";
	}

	private String callId(ApplicationArguments args, String commandName) {
		if (!args.containsOption("call-id")) {
			throw new IllegalArgumentException("Provide --call-id=<local_call_id> with " + commandName);
		}

		List<String> callIdValues = args.getOptionValues("call-id");
		if (callIdValues == null || callIdValues.isEmpty() || callIdValues.getFirst().isBlank()) {
			throw new IllegalArgumentException("Provide --call-id=<local_call_id> with " + commandName);
		}
		return callIdValues.getFirst();
	}

	private void listRuns(ApplicationArguments args) {
		RunFilters filters = new RunFilters(
				optionValue(args, "scenario"),
				optionValue(args, "status"),
				optionValue(args, "run-mode")
		);
		List<RunIndexEntry> entries = runInspectionService == null
				? runIndexWriter.readAll()
				: runInspectionService.listRuns(filters);
		if (entries.isEmpty()) {
			System.out.println("No runs matched.");
			return;
		}

		System.out.println("call_id | scenario_id | run_mode | status | complete | warnings");
		for (RunIndexEntry entry : entries) {
			System.out.println(String.join(" | ",
					entry.callId(),
					entry.scenarioId(),
					entry.runMode(),
					entry.status(),
					Boolean.toString(entry.complete()),
					String.join(", ", entry.warnings())
			));
		}
	}

	private void showRun(String callId) {
		RunInspection inspection = runInspectionService.showRun(callId);
		RunMetadata metadata = inspection.metadata();
		ArtifactCompleteness completeness = inspection.completeness();
		System.out.println("Run inspection");
		System.out.println("call_id: " + metadata.callId());
		System.out.println("scenario_id: " + metadata.scenarioId());
		System.out.println("run_mode: " + metadata.runMode());
		System.out.println("status: " + metadata.status());
		System.out.println("retell_call_id: " + valueOrNone(metadata.retellCallId()));
		System.out.println("run_directory: " + inspection.runDirectory());
		if (metadata.analysis() != null) {
			System.out.println("analysis_provider: " + metadata.analysis().provider());
			System.out.println("analysis_model: " + metadata.analysis().model());
		}
		if (metadata.evaluation() != null) {
			System.out.println("evaluation_provider: " + metadata.evaluation().provider());
			System.out.println("evaluation_model: " + metadata.evaluation().model());
		}
		System.out.println("complete: " + completeness.complete());
		if (!completeness.missingRequiredArtifacts().isEmpty()) {
			System.out.println("missing_required_artifacts: "
					+ String.join(", ", completeness.missingRequiredArtifacts()));
		}
		if (!completeness.warnings().isEmpty()) {
			System.out.println("warnings: " + String.join(", ", completeness.warnings()));
		}
		System.out.println("artifact_paths:");
		printArtifactPaths(metadata.artifactPaths());
		System.out.println("next_steps:");
		for (String nextStep : nextSteps(metadata, completeness)) {
			System.out.println("- " + nextStep);
		}
		if (inspection.latestIndexEntry() == null) {
			System.out.println("index: no index entry found for this call_id");
		}
	}

	private void printArtifactPaths(ArtifactPaths paths) {
		printPath("scenario", paths.scenario());
		printPath("metadata", paths.metadata());
		printPath("transcript_text", paths.transcriptText());
		printPath("transcript_json", paths.transcriptJson());
		printPath("patient_simulation", paths.patientSimulation());
		printPath("audio", paths.audio());
		printPath("manifest", paths.manifest());
		printPath("analysis_json", paths.analysisJson());
		printPath("analysis_markdown", paths.analysisMarkdown());
		printPath("evaluation_json", paths.evaluationJson());
		printPath("evaluation_markdown", paths.evaluationMarkdown());
		printPath("multi_lens_review_json", paths.multiLensReviewJson());
		printPath("multi_lens_review_markdown", paths.multiLensReviewMarkdown());
		printPath("observations_markdown", paths.observationsMarkdown());
	}

	private void printPath(String name, String path) {
		System.out.println("- " + name + ": " + valueOrNone(path));
	}

	private List<String> nextSteps(RunMetadata metadata, ArtifactCompleteness completeness) {
		if ("retell".equals(metadata.runMode()) && isBlank(metadata.artifactPaths().transcriptJson())) {
			return List.of("Capture Retell artifacts with --capture-artifacts --call-id=" + metadata.callId());
		}
		if (!hasArtifact(completeness, "observations_markdown")) {
			return List.of("Refresh observations with --review-conversation --call-id=" + metadata.callId());
		}
		if (hasArtifact(completeness, "transcript_json") && isBlank(metadata.artifactPaths().analysisJson())) {
			return List.of("Analyze captured transcript with --analyze-call --call-id=" + metadata.callId());
		}
		if (hasArtifact(completeness, "transcript_json") && isBlank(metadata.artifactPaths().evaluationJson())) {
			return List.of("Evaluate captured transcript with --evaluate-call --call-id=" + metadata.callId());
		}
		if (hasArtifact(completeness, "transcript_json")
				&& isBlank(metadata.artifactPaths().multiLensReviewJson())) {
			return List.of("Run multi-lens review with --multi-lens-review --call-id=" + metadata.callId());
		}
		return List.of("Inspect artifacts under " + metadata.artifactPaths().metadata());
	}

	private boolean hasArtifact(ArtifactCompleteness completeness, String name) {
		for (ArtifactStatus status : completeness.artifacts()) {
			if (name.equals(status.name())) {
				return status.present();
			}
		}
		return false;
	}

	private void printHelp() {
		System.out.println("Voice AI QA Agent");
		System.out.println("Commands:");
		System.out.println("- --scenario=<path> [--run-mode=dry-run|retell]");
		System.out.println("- --capture-artifacts --call-id=<local_call_id>");
		System.out.println("- --review-conversation --call-id=<local_call_id>");
		System.out.println("- --analyze-call --call-id=<local_call_id>");
		System.out.println("- --evaluate-call --call-id=<local_call_id>");
		System.out.println("- --multi-lens-review --call-id=<local_call_id>");
		System.out.println("- --generate-report");
		System.out.println("- --generate-scenarios --agent-description=<description> [--scenario-count=<count>]");
		System.out.println("- --show-run --call-id=<local_call_id>");
		System.out.println("- --list-runs [--scenario=<scenario_id>] [--status=<status>] [--run-mode=dry-run|retell]");
	}

	private String agentDescription(ApplicationArguments args) {
		String value = optionValue(args, "agent-description");
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException(
					"Provide --agent-description=<description> with --generate-scenarios"
			);
		}
		return value;
	}

	private Integer scenarioCount(ApplicationArguments args) {
		String value = optionValue(args, "scenario-count");
		if (value == null || value.isBlank()) {
			return null;
		}
		try {
			return Integer.parseInt(value);
		} catch (NumberFormatException exception) {
			throw new IllegalArgumentException("scenario-count must be a number", exception);
		}
	}

	private String optionValue(ApplicationArguments args, String optionName) {
		if (!args.containsOption(optionName)) {
			return null;
		}
		List<String> values = args.getOptionValues(optionName);
		if (values == null || values.isEmpty()) {
			return null;
		}
		return values.getFirst();
	}

	private String valueOrNone(String value) {
		return isBlank(value) ? "(none)" : value;
	}

	private boolean isBlank(String value) {
		return value == null || value.isBlank();
	}

	private String failureContext(ApplicationArguments args) {
		StringBuilder context = new StringBuilder("command=").append(commandName(args));
		if (args.containsOption("call-id")) {
			List<String> callIdValues = args.getOptionValues("call-id");
			if (callIdValues != null && !callIdValues.isEmpty() && !callIdValues.getFirst().isBlank()) {
				context.append(" call_id=").append(callIdValues.getFirst());
			}
		}
		if (args.containsOption("scenario")) {
			List<String> scenarioValues = args.getOptionValues("scenario");
			if (scenarioValues != null && !scenarioValues.isEmpty() && !scenarioValues.getFirst().isBlank()) {
				context.append(" scenario=").append(scenarioValues.getFirst());
			}
		}
		return context.toString();
	}

	private String commandName(ApplicationArguments args) {
		if (args.containsOption("list-runs")) {
			return "list-runs";
		}
		if (args.containsOption("show-run")) {
			return "show-run";
		}
		if (args.containsOption("analyze-call")) {
			return "analyze-call";
		}
		if (args.containsOption("evaluate-call")) {
			return "evaluate-call";
		}
		if (args.containsOption("multi-lens-review")) {
			return "multi-lens-review";
		}
		if (args.containsOption("generate-report")) {
			return "generate-report";
		}
		if (args.containsOption("generate-scenarios")) {
			return "generate-scenarios";
		}
		if (args.containsOption("review-conversation")) {
			return "review-conversation";
		}
		if (args.containsOption("capture-artifacts")) {
			return "capture-artifacts";
		}
		if (args.containsOption("scenario")) {
			return "scenario";
		}
		return "none";
	}
}
