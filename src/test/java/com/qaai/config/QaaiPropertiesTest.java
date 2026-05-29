package com.qaai.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
		"qaai.retell.api-key=test-retell-key",
		"qaai.retell.agent-id=agent_123",
		"qaai.retell.from-number=+15555550100",
		"qaai.retell.base-url=https://api.example.test",
		"qaai.retell.api-timeout=12s",
		"qaai.retell.recording-download-timeout=34s",
		"qaai.analysis.provider=local",
		"qaai.scenario-generation.provider=openai",
		"qaai.review.provider=local",
		"qaai.openai.api-key=test-openai-key",
		"qaai.openai.analysis-model=test-analysis-model",
		"qaai.openai.analysis-timeout=56s",
		"qaai.openai.scenario-generation-model=test-scenario-model",
		"qaai.openai.scenario-generation-timeout=78s",
		"qaai.target.agent-phone-number=+18054398008",
		"qaai.outputs.base-dir=outputs"
})
class QaaiPropertiesTest {

	@Autowired
	private QaaiProperties properties;

	@Test
	void bindsQaaiConfiguration() {
		assertThat(properties.retell().apiKey()).isEqualTo("test-retell-key");
		assertThat(properties.retell().agentId()).isEqualTo("agent_123");
		assertThat(properties.retell().fromNumber()).isEqualTo("+15555550100");
		assertThat(properties.retell().baseUrl()).isEqualTo("https://api.example.test");
		assertThat(properties.retell().apiTimeout()).isEqualTo(Duration.ofSeconds(12));
		assertThat(properties.retell().recordingDownloadTimeout()).isEqualTo(Duration.ofSeconds(34));
		assertThat(properties.analysis().provider()).isEqualTo("local");
		assertThat(properties.scenarioGeneration().provider()).isEqualTo("openai");
		assertThat(properties.review().provider()).isEqualTo("local");
		assertThat(properties.openai().apiKey()).isEqualTo("test-openai-key");
		assertThat(properties.openai().analysisModel()).isEqualTo("test-analysis-model");
		assertThat(properties.openai().analysisTimeout()).isEqualTo(Duration.ofSeconds(56));
		assertThat(properties.openai().scenarioGenerationModel()).isEqualTo("test-scenario-model");
		assertThat(properties.openai().scenarioGenerationTimeout()).isEqualTo(Duration.ofSeconds(78));
		assertThat(properties.target().agentPhoneNumber()).isEqualTo("+18054398008");
		assertThat(properties.outputs().baseDir()).isEqualTo("outputs");
	}
}
