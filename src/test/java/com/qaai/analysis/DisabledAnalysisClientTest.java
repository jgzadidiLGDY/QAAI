package com.qaai.analysis;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class DisabledAnalysisClientTest {

	@Test
	void rejectsAnalysisWhenProviderIsDisabled() {
		DisabledAnalysisClient client = new DisabledAnalysisClient();

		assertThatThrownBy(() -> client.analyze(new AnalysisRequest(null, null, "prompt")))
				.isInstanceOf(AnalysisException.class)
				.hasMessageContaining("Analysis is disabled");
	}
}
