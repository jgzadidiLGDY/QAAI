package com.qaai.runner;

import java.nio.file.Path;
import java.util.List;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class ScenarioRunnerCommand implements ApplicationRunner {

	private final DryRunRunner dryRunRunner;
	private final RetellCallRunner retellCallRunner;

	public ScenarioRunnerCommand(DryRunRunner dryRunRunner, RetellCallRunner retellCallRunner) {
		this.dryRunRunner = dryRunRunner;
		this.retellCallRunner = retellCallRunner;
	}

	@Override
	public void run(ApplicationArguments args) {
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
}
