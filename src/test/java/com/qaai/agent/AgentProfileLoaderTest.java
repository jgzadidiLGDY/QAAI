package com.qaai.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class AgentProfileLoaderTest {

	@Test
	void loadsValidAgentProfile() {
		AgentProfile profile = new AgentProfileLoader().load(
				Path.of("agent-profiles/medical-receptionist-demo.yaml")
		);

		assertThat(profile.id()).isEqualTo("medical_receptionist_demo");
		assertThat(profile.supportedWorkflows()).contains("appointment_scheduling");
		assertThat(profile.channels()).containsKeys("voice", "text");
		assertThat(profile.channels().get("voice").targetPhoneNumber()).isEqualTo("+18054398008");
		new AgentProfileValidator().validate(profile);
	}

	@Test
	void rejectsMissingRequiredFields() {
		AgentProfile profile = new AgentProfile(
				"",
				"Medical Receptionist Demo",
				"healthcare",
				"",
				null,
				null
		);

		assertThatThrownBy(() -> new AgentProfileValidator().validate(profile))
				.isInstanceOf(AgentProfileValidationException.class)
				.hasMessageContaining("id is required")
				.hasMessageContaining("supported_workflows must include at least one item")
				.hasMessageContaining("channels must include at least one channel");
	}
}
