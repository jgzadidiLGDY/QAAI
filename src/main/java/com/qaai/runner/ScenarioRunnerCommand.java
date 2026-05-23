package com.qaai.runner;

import java.nio.file.Path;
import java.util.List;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class ScenarioRunnerCommand implements ApplicationRunner {

	private final DryRunRunner dryRunRunner;

	public ScenarioRunnerCommand(DryRunRunner dryRunRunner) {
		this.dryRunRunner = dryRunRunner;
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
		DryRunResult result = dryRunRunner.run(Path.of(scenarioPath));
		System.out.println("Dry run completed");
		System.out.println("call_id: " + result.metadata().callId());
		System.out.println("artifacts: " + result.artifacts().runDirectory());
	}
}
