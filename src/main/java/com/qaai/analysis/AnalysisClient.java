package com.qaai.analysis;

public interface AnalysisClient {

	AnalysisReport analyze(AnalysisRequest request);

	default AnalysisReport analyze(String prompt) {
		return analyze(new AnalysisRequest(null, null, prompt));
	}

	default String provider() {
		return "unknown";
	}

	default String model() {
		return null;
	}
}
