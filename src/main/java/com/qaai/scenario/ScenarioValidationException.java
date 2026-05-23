package com.qaai.scenario;

import java.util.List;

public class ScenarioValidationException extends RuntimeException {

	private final List<String> errors;

	public ScenarioValidationException(List<String> errors) {
		super("Scenario validation failed: " + String.join("; ", errors));
		this.errors = List.copyOf(errors);
	}

	public List<String> errors() {
		return errors;
	}
}
