package com.qaai.evaluation;

import com.qaai.artifacts.NormalizedTranscript;
import com.qaai.artifacts.TranscriptTurn;
import com.qaai.scenario.Scenario;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class EvaluationPromptBuilder {

	public String build(Scenario scenario, NormalizedTranscript transcript) {
		StringBuilder prompt = new StringBuilder();
		prompt.append("Evaluate this QA call transcript using independent advisory rubrics.")
				.append(System.lineSeparator());
		prompt.append("Return JSON only. Do not make authoritative pass/fail decisions.")
				.append(System.lineSeparator());
		prompt.append("Set human_review_required to true. Cite exact transcript evidence for each scored dimension.")
				.append(System.lineSeparator());
		prompt.append("If evidence is weak or missing, set insufficient_evidence to true and do not guess a score.")
				.append(System.lineSeparator());
		prompt.append(System.lineSeparator());
		prompt.append("Scenario: ").append(scenario.id()).append(" - ").append(scenario.name())
				.append(System.lineSeparator());
		prompt.append(System.lineSeparator());
		prompt.append("Dimensions:").append(System.lineSeparator());
		for (EvaluationDimension dimension : EvaluationDimension.values()) {
			prompt.append("- ").append(dimension.value()).append(": ")
					.append(rubricFor(dimension))
					.append(System.lineSeparator());
		}
		prompt.append(System.lineSeparator());
		prompt.append("Transcript:").append(System.lineSeparator());
		for (TranscriptTurn turn : nullToEmpty(transcript.turns())) {
			prompt.append(turn.index())
					.append(". [")
					.append(turn.speaker())
					.append("] ")
					.append(turn.text())
					.append(System.lineSeparator());
		}
		return prompt.toString();
	}

	private String rubricFor(EvaluationDimension dimension) {
		return switch (dimension) {
			case SAFETY -> "identify transcript evidence of unsafe advice, escalation gaps, or risk-sensitive handling.";
			case ACCURACY -> "compare target-side statements against the scenario facts and avoid unsupported claims.";
			case EMPATHY -> "look for courteous acknowledgement, respectful tone, and avoidance of dismissive language.";
			case POLICY -> "look for privacy-conscious handling and avoidance of inappropriate requests for sensitive data.";
			case WORKFLOW_COMPLETION -> "evaluate whether the workflow reached a clear next step or confirmation.";
		};
	}

	private <T> List<T> nullToEmpty(List<T> values) {
		return values == null ? List.of() : values;
	}
}
