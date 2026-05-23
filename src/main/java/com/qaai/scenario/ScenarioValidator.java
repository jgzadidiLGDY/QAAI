package com.qaai.scenario;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ScenarioValidator {

	public void validate(Scenario scenario) {
		List<String> errors = new ArrayList<>();

		if (scenario == null) {
			throw new ScenarioValidationException(List.of("scenario is required"));
		}

		requireText(errors, "id", scenario.id());
		requireText(errors, "name", scenario.name());
		requireText(errors, "workflow", scenario.workflow());

		if (scenario.persona() == null) {
			errors.add("persona is required");
		} else {
			requireText(errors, "persona.name", scenario.persona().name());
		}

		if (scenario.goal() == null) {
			errors.add("goal is required");
		} else {
			requireText(errors, "goal.summary", scenario.goal().summary());
		}

		if (scenario.steps() == null || scenario.steps().isEmpty()) {
			errors.add("steps must include at least one step");
		} else {
			for (int index = 0; index < scenario.steps().size(); index++) {
				Scenario.Step step = scenario.steps().get(index);
				if (step == null) {
					errors.add("steps[" + index + "] is required");
				} else {
					requireText(errors, "steps[" + index + "].intent", step.intent());
					requireText(errors, "steps[" + index + "].patient_says", step.patientSays());
				}
			}
		}

		if (!errors.isEmpty()) {
			throw new ScenarioValidationException(errors);
		}
	}

	private static void requireText(List<String> errors, String field, String value) {
		if (value == null || value.isBlank()) {
			errors.add(field + " is required");
		}
	}
}
