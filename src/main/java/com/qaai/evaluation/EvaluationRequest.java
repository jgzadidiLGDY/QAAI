package com.qaai.evaluation;

import com.qaai.artifacts.NormalizedTranscript;
import com.qaai.scenario.Scenario;

public record EvaluationRequest(
		Scenario scenario,
		NormalizedTranscript transcript,
		String prompt
) {
}
