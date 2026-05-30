package com.qaai.suite;

import java.util.List;

public class ScenarioSuiteValidationException extends RuntimeException {

	private final List<String> errors;

	public ScenarioSuiteValidationException(List<String> errors) {
		super("Scenario suite validation failed: " + String.join("; ", errors));
		this.errors = List.copyOf(errors);
	}

	public List<String> errors() {
		return errors;
	}
}
