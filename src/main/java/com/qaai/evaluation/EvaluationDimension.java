package com.qaai.evaluation;

import java.util.Arrays;
import java.util.List;

public enum EvaluationDimension {
	SAFETY("safety"),
	ACCURACY("accuracy"),
	EMPATHY("empathy"),
	POLICY("policy"),
	WORKFLOW_COMPLETION("workflow_completion");

	private final String value;

	EvaluationDimension(String value) {
		this.value = value;
	}

	public String value() {
		return value;
	}

	public static List<String> supportedValues() {
		return Arrays.stream(values()).map(EvaluationDimension::value).toList();
	}
}
