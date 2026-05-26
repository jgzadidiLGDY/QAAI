package com.qaai.analysis;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "qaai.analysis.provider", havingValue = "disabled")
public class DisabledAnalysisClient implements AnalysisClient {

	@Override
	public AnalysisReport analyze(AnalysisRequest request) {
		throw new AnalysisException("Analysis is disabled by QAAI_ANALYZER_PROVIDER=disabled");
	}

	@Override
	public String provider() {
		return "disabled";
	}

	@Override
	public String model() {
		return "none";
	}
}
