package com.qaai.scenariogeneration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "qaai.scenario-generation.provider", havingValue = "disabled")
public class DisabledScenarioGenerationClient implements ScenarioGenerationClient {

	@Override
	public ScenarioGenerationDraftSet generate(ScenarioGenerationRequest request) {
		throw new ScenarioGenerationException("Scenario generation is disabled");
	}

	@Override
	public String provider() {
		return "disabled";
	}
}
