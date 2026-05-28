package com.qaai.scenariogeneration;

public interface ScenarioGenerationClient {

	ScenarioGenerationDraftSet generate(ScenarioGenerationRequest request);

	default String provider() {
		return "unknown";
	}

	default String model() {
		return null;
	}
}
