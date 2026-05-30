package com.qaai.agent;

import java.util.List;

public class AgentProfileValidationException extends RuntimeException {

	private final List<String> errors;

	public AgentProfileValidationException(List<String> errors) {
		super("Agent profile validation failed: " + String.join("; ", errors));
		this.errors = List.copyOf(errors);
	}

	public List<String> errors() {
		return errors;
	}
}
