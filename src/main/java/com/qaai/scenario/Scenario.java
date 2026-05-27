package com.qaai.scenario;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record Scenario(
		String id,
		String name,
		String workflow,
		Persona persona,
		Goal goal,
		Constraints constraints,
		Coverage coverage,
		@JsonProperty("conversation_quality")
		ConversationQuality conversationQuality,
		List<Step> steps
) {

	public record Persona(
			String name,
			@JsonProperty("date_of_birth")
			String dateOfBirth,
			@JsonProperty("phone_number")
			String phoneNumber
	) {
	}

	public record Goal(
			@JsonProperty("call_reason")
			String callReason,
			String summary,
			@JsonProperty("expected_outcome")
			String expectedOutcome
	) {
	}

	public record Constraints(
			@JsonProperty("allowed_facts")
			List<String> allowedFacts,
			@JsonProperty("disallowed_behavior")
			List<String> disallowedBehavior
	) {
	}

	public record Coverage(
			@JsonProperty("workflow_area")
			String workflowArea,
			@JsonProperty("edge_cases")
			List<String> edgeCases,
			@JsonProperty("risk_focus")
			String riskFocus
	) {
	}

	public record ConversationQuality(
			@JsonProperty("welcome_behavior")
			String welcomeBehavior,
			@JsonProperty("initiative")
			String initiative,
			@JsonProperty("pacing")
			String pacing,
			@JsonProperty("clarification")
			String clarification,
			@JsonProperty("expected_risks")
			List<String> expectedRisks
	) {
	}

	public record Step(
			String intent,
			@JsonProperty("patient_says")
			String patientSays
	) {
	}
}
