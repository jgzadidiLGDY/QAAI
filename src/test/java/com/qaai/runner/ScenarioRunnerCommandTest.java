package com.qaai.runner;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;

class ScenarioRunnerCommandTest {

	@Test
	void reviewConversationRequiresCallId() {
		ScenarioRunnerCommand command = new ScenarioRunnerCommand(null, null, null, null, null, null);

		command.run(new DefaultApplicationArguments("--review-conversation"));

		assertThat(command.getExitCode()).isEqualTo(1);
	}
}
