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

	public record Step(
			String intent,
			@JsonProperty("patient_says")
			String patientSays
	) {
	}
}
