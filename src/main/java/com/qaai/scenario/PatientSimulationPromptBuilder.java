package com.qaai.scenario;

import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class PatientSimulationPromptBuilder {

	public String build(Scenario scenario) {
		StringBuilder prompt = new StringBuilder();
		prompt.append("# Patient Simulation Scenario").append(System.lineSeparator());
		prompt.append(System.lineSeparator());
		prompt.append("Scenario ID: ").append(scenario.id()).append(System.lineSeparator());
		prompt.append("Scenario name: ").append(scenario.name()).append(System.lineSeparator());
		prompt.append("Workflow: ").append(scenario.workflow()).append(System.lineSeparator());
		prompt.append(System.lineSeparator());

		prompt.append("## Patient").append(System.lineSeparator());
		prompt.append("- Name: ").append(scenario.persona().name()).append(System.lineSeparator());
		prompt.append("- Date of birth: ").append(valueOrUnavailable(scenario.persona().dateOfBirth()))
				.append(System.lineSeparator());
		prompt.append("- Phone number: ").append(valueOrUnavailable(scenario.persona().phoneNumber()))
				.append(System.lineSeparator());
		prompt.append(System.lineSeparator());

		prompt.append("## Goal").append(System.lineSeparator());
		prompt.append("- Call reason: ").append(scenario.goal().callReason()).append(System.lineSeparator());
		prompt.append("- Summary: ").append(scenario.goal().summary()).append(System.lineSeparator());
		prompt.append("- Success condition: ").append(scenario.goal().expectedOutcome()).append(System.lineSeparator());
		prompt.append(System.lineSeparator());

		prompt.append("## Known Facts").append(System.lineSeparator());
		appendBullets(prompt, scenario.constraints().allowedFacts());
		prompt.append(System.lineSeparator());

		prompt.append("## Must Not Provide Or Invent").append(System.lineSeparator());
		appendBullets(prompt, scenario.constraints().disallowedBehavior());
		prompt.append(System.lineSeparator());

		prompt.append("## Conversation Behavior").append(System.lineSeparator());
		prompt.append("- Welcome behavior: ").append(scenario.conversationQuality().welcomeBehavior())
				.append(System.lineSeparator());
		prompt.append("- Initiative: ").append(scenario.conversationQuality().initiative())
				.append(System.lineSeparator());
		prompt.append("- Pacing: ").append(scenario.conversationQuality().pacing()).append(System.lineSeparator());
		prompt.append("- Clarification: ").append(scenario.conversationQuality().clarification())
				.append(System.lineSeparator());
		prompt.append(System.lineSeparator());

		prompt.append("## Expected Risks To Avoid").append(System.lineSeparator());
		appendBullets(prompt, scenario.conversationQuality().expectedRisks());
		prompt.append(System.lineSeparator());

		prompt.append("## Suggested Patient Turns").append(System.lineSeparator());
		for (int index = 0; index < scenario.steps().size(); index++) {
			Scenario.Step step = scenario.steps().get(index);
			prompt.append(index + 1)
					.append(". ")
					.append(step.patientSays())
					.append(" [intent: ")
					.append(step.intent())
					.append("]")
					.append(System.lineSeparator());
		}
		prompt.append(System.lineSeparator());

		prompt.append("Use the suggested turns as the planned patient path, but answer direct questions naturally ")
				.append("using only the known facts above. Do not claim success unless the success condition is clearly met.")
				.append(System.lineSeparator());

		return prompt.toString();
	}

	private void appendBullets(StringBuilder prompt, List<String> values) {
		for (String value : values) {
			prompt.append("- ").append(value).append(System.lineSeparator());
		}
	}

	private String valueOrUnavailable(String value) {
		if (value == null || value.isBlank()) {
			return "not provided";
		}
		return value;
	}
}
