package com.qaai.runner;

import com.qaai.analysis.AnalysisResult;
import com.qaai.analysis.AnalysisService;
import com.qaai.artifacts.RunIndexEntry;
import com.qaai.artifacts.RunIndexWriter;
import com.qaai.quality.ConversationQualityReviewResult;
import com.qaai.quality.ConversationQualityReviewService;
import java.nio.file.Path;
import java.util.List;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.stereotype.Component;

@Component
public class ScenarioRunnerCommand implements ApplicationRunner, ExitCodeGenerator {

	private final DryRunRunner dryRunRunner;
	private final RetellCallRunner retellCallRunner;
	private final ArtifactCaptureService artifactCaptureService;
	private final AnalysisService analysisService;
	private final RunIndexWriter runIndexWriter;
	private final ConversationQualityReviewService conversationQualityReviewService;
	private int exitCode;

	public ScenarioRunnerCommand(
			DryRunRunner dryRunRunner,
			RetellCallRunner retellCallRunner,
			ArtifactCaptureService artifactCaptureService,
			AnalysisService analysisService,
			RunIndexWriter runIndexWriter,
			ConversationQualityReviewService conversationQualityReviewService
	) {
		this.dryRunRunner = dryRunRunner;
		this.retellCallRunner = retellCallRunner;
		this.artifactCaptureService = artifactCaptureService;
		this.analysisService = analysisService;
		this.runIndexWriter = runIndexWriter;
		this.conversationQualityReviewService = conversationQualityReviewService;
	}

	@Override
	public void run(ApplicationArguments args) {
		try {
			runCommand(args);
		} catch (RuntimeException exception) {
			exitCode = 1;
			System.err.println("Error: " + exception.getMessage());
		}
	}

	@Override
	public int getExitCode() {
		return exitCode;
	}

	private void runCommand(ApplicationArguments args) {
		if (args.containsOption("list-runs")) {
			listRuns();
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
			return;
		}

		if (!args.containsOption("scenario")) {
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

	private void listRuns() {
		List<RunIndexEntry> entries = runIndexWriter.readAll();
		if (entries.isEmpty()) {
			System.out.println("No runs indexed yet.");
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
}
