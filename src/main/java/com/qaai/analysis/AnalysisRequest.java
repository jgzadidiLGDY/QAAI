package com.qaai.analysis;

import com.qaai.artifacts.NormalizedTranscript;
import com.qaai.scenario.Scenario;

public record AnalysisRequest(
		Scenario scenario,
		NormalizedTranscript transcript,
		String prompt
) {
}
