package com.qaai.suite;

import com.qaai.agent.AgentProfile;
import com.qaai.scenario.Scenario;
import com.qaai.scenario.ScenarioLoader;
import com.qaai.scenario.ScenarioValidationException;
import com.qaai.scenario.ScenarioValidator;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ScenarioSuiteValidator {

	private static final List<String> SUPPORTED_RUN_MODES = List.of("text-chat");

	private final ScenarioLoader scenarioLoader;
	private final ScenarioValidator scenarioValidator;

	public ScenarioSuiteValidator(ScenarioLoader scenarioLoader, ScenarioValidator scenarioValidator) {
		this.scenarioLoader = scenarioLoader;
		this.scenarioValidator = scenarioValidator;
	}

	public void validate(ScenarioSuite suite, AgentProfile profile) {
		List<String> errors = new ArrayList<>();
		if (suite == null) {
			throw new ScenarioSuiteValidationException(List.of("scenario suite is required"));
		}

		requireText(errors, "id", suite.id());
		requireText(errors, "agent_profile", suite.agentProfile());
		requireText(errors, "default_run_mode", suite.defaultRunMode());
		if (profile == null) {
			errors.add("agent_profile did not resolve to a valid profile");
		} else if (suite.agentProfile() != null && !suite.agentProfile().equals(profile.id())) {
			errors.add("agent_profile must match loaded profile id");
		}
		if (suite.defaultRunMode() != null && !SUPPORTED_RUN_MODES.contains(suite.defaultRunMode())) {
			errors.add("default_run_mode must be one of " + SUPPORTED_RUN_MODES);
		}
		validateScenarios(errors, suite.scenarios());

		if (!errors.isEmpty()) {
			throw new ScenarioSuiteValidationException(errors);
		}
	}

	private void validateScenarios(List<String> errors, List<String> scenarios) {
		if (scenarios == null || scenarios.isEmpty()) {
			errors.add("scenarios must include at least one item");
			return;
		}
		for (int index = 0; index < scenarios.size(); index++) {
			String scenarioPath = scenarios.get(index);
			if (scenarioPath == null || scenarioPath.isBlank()) {
				errors.add("scenarios[" + index + "] is required");
				continue;
			}
			Path path = Path.of(scenarioPath);
			if (!Files.exists(path)) {
				errors.add("scenarios[" + index + "] does not exist: " + scenarioPath);
				continue;
			}
			try {
				Scenario scenario = scenarioLoader.load(path);
				scenarioValidator.validate(scenario);
			} catch (ScenarioValidationException exception) {
				errors.add("scenarios[" + index + "] is invalid: " + exception.getMessage());
			} catch (RuntimeException exception) {
				errors.add("scenarios[" + index + "] could not be loaded: " + exception.getMessage());
			}
		}
	}

	private static void requireText(List<String> errors, String field, String value) {
		if (value == null || value.isBlank()) {
			errors.add(field + " is required");
		}
	}
}
