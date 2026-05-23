package com.qaai.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
		"qaai.retell.api-key=test-retell-key",
		"qaai.retell.agent-id=agent_123",
		"qaai.retell.from-number=+15555550100",
		"qaai.retell.base-url=https://api.example.test",
		"qaai.openai.api-key=test-openai-key",
		"qaai.openai.analysis-model=test-analysis-model",
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
		assertThat(properties.openai().apiKey()).isEqualTo("test-openai-key");
		assertThat(properties.openai().analysisModel()).isEqualTo("test-analysis-model");
		assertThat(properties.target().agentPhoneNumber()).isEqualTo("+18054398008");
		assertThat(properties.outputs().baseDir()).isEqualTo("outputs");
	}
}
