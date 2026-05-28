package com.qaai.evaluation;

public interface EvaluationClient {

	EvaluationReport evaluate(EvaluationRequest request);

	default String provider() {
		return "unknown";
	}

	default String model() {
		return null;
	}
}
