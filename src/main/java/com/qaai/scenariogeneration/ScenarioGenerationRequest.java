package com.qaai.scenariogeneration;

public record ScenarioGenerationRequest(
		String generationId,
		String agentDescription,
		int scenarioCount,
		String prompt
) {
}
