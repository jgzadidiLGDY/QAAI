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
			requireText(errors, "goal.call_reason", scenario.goal().callReason());
			requireText(errors, "goal.summary", scenario.goal().summary());
			requireText(errors, "goal.expected_outcome", scenario.goal().expectedOutcome());
		}

		if (scenario.constraints() == null) {
			errors.add("constraints is required");
		} else {
			requireNonEmpty(errors, "constraints.allowed_facts", scenario.constraints().allowedFacts());
			requireNonEmpty(errors, "constraints.disallowed_behavior", scenario.constraints().disallowedBehavior());
		}

		if (scenario.conversationQuality() == null) {
			errors.add("conversation_quality is required");
		} else {
			requireText(errors, "conversation_quality.welcome_behavior",
					scenario.conversationQuality().welcomeBehavior());
			requireText(errors, "conversation_quality.initiative", scenario.conversationQuality().initiative());
			requireText(errors, "conversation_quality.pacing", scenario.conversationQuality().pacing());
			requireText(errors, "conversation_quality.clarification",
					scenario.conversationQuality().clarification());
			requireNonEmpty(errors, "conversation_quality.expected_risks",
					scenario.conversationQuality().expectedRisks());
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

	private static void requireNonEmpty(List<String> errors, String field, List<String> values) {
		if (values == null || values.isEmpty()) {
			errors.add(field + " must include at least one item");
		}
	}
}
