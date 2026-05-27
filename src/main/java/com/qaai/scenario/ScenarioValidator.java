package com.qaai.scenario;

import java.util.ArrayList;
import java.util.Set;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ScenarioValidator {

	private static final Set<String> ALLOWED_EDGE_CASES = Set.of(
			"happy_path",
			"missing_fact",
			"clarification",
			"transfer_or_hold",
			"ambiguous_next_step",
			"unavailable_information",
			"workflow_recovery",
			"workflow_mismatch"
	);

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

		if (scenario.coverage() == null) {
			errors.add("coverage is required");
		} else {
			requireText(errors, "coverage.workflow_area", scenario.coverage().workflowArea());
			requireNonEmpty(errors, "coverage.edge_cases", scenario.coverage().edgeCases());
			requireText(errors, "coverage.risk_focus", scenario.coverage().riskFocus());
			validateEdgeCases(errors, scenario.coverage().edgeCases());
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

	private static void validateEdgeCases(List<String> errors, List<String> edgeCases) {
		if (edgeCases == null) {
			return;
		}
		for (int index = 0; index < edgeCases.size(); index++) {
			String edgeCase = edgeCases.get(index);
			if (edgeCase == null || edgeCase.isBlank()) {
				errors.add("coverage.edge_cases[" + index + "] is required");
			} else if (!ALLOWED_EDGE_CASES.contains(edgeCase)) {
				errors.add("coverage.edge_cases[" + index + "] must be one of " + ALLOWED_EDGE_CASES);
			}
		}
	}
}
