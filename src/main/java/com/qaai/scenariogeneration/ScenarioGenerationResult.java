package com.qaai.scenariogeneration;

import java.nio.file.Path;

public record ScenarioGenerationResult(
		String generationId,
		Path generationDirectory,
		Path agentDescription,
		Path coveragePlan,
		Path generationReportJson,
		Path generationReportMarkdown
) {
}
