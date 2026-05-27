package com.qaai.runner;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

@ExtendWith(OutputCaptureExtension.class)
class ScenarioRunnerCommandTest {

	@Test
	void reviewConversationRequiresCallId() {
		ScenarioRunnerCommand command = new ScenarioRunnerCommand(null, null, null, null, null, null, null);

		command.run(new DefaultApplicationArguments("--review-conversation"));

		assertThat(command.getExitCode()).isEqualTo(1);
	}

	@Test
	void printsHelpWhenNoCommandIsProvided(CapturedOutput output) {
		ScenarioRunnerCommand command = new ScenarioRunnerCommand(null, null, null, null, null, null, null);

		command.run(new DefaultApplicationArguments());

		assertThat(command.getExitCode()).isZero();
		assertThat(output).contains("--show-run --call-id=<local_call_id>");
		assertThat(output).contains("--list-runs [--scenario=<scenario_id>]");
	}
}
