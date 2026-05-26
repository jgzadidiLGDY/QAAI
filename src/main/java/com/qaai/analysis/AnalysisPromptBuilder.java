package com.qaai.analysis;

import com.qaai.artifacts.NormalizedTranscript;
import com.qaai.artifacts.TranscriptTurn;
import com.qaai.scenario.Scenario;
import org.springframework.stereotype.Component;

@Component
public class AnalysisPromptBuilder {

	public String build(Scenario scenario, NormalizedTranscript transcript) {
		StringBuilder prompt = new StringBuilder();
		prompt.append("You are assisting a human QA reviewer for a healthcare voice-agent test.")
				.append(System.lineSeparator());
		prompt.append("Analyze the stored transcript against the stored scenario expectations.")
				.append(System.lineSeparator());
		prompt.append("Do not make an authoritative pass/fail decision. Human review owns final judgment.")
				.append(System.lineSeparator());
		prompt.append("Every finding must cite exact transcript evidence from the provided turns.")
				.append(System.lineSeparator());
		prompt.append("If the transcript does not support a finding, omit the finding.")
				.append(System.lineSeparator());
		prompt.append("Return only valid JSON with this shape:")
				.append(System.lineSeparator());
		prompt.append("""
				{
				  "call_id": "string",
				  "scenario_id": "string",
				  "summary": "string",
				  "human_review_required": true,
				  "findings": [
				    {
				      "title": "string",
				      "severity": "low|medium|high|critical",
				      "workflow": "string",
				      "expected_behavior": "string",
				      "actual_behavior": "string",
				      "evidence": [
				        {
				          "artifact": "transcript.txt",
				          "speaker": "patient|receptionist|unknown",
				          "quote": "exact quote from one transcript turn",
				          "timestamp": null
				        }
				      ]
				    }
				  ],
				  "notes": ["string"]
				}
				""");
		prompt.append(System.lineSeparator());
		prompt.append("Scenario").append(System.lineSeparator());
		prompt.append("call_id: ").append(transcript.callId()).append(System.lineSeparator());
		prompt.append("scenario_id: ").append(scenario.id()).append(System.lineSeparator());
		prompt.append("name: ").append(scenario.name()).append(System.lineSeparator());
		prompt.append("workflow: ").append(scenario.workflow()).append(System.lineSeparator());
		prompt.append("goal_summary: ").append(scenario.goal().summary()).append(System.lineSeparator());
		prompt.append("expected_outcome: ").append(scenario.goal().expectedOutcome()).append(System.lineSeparator());
		prompt.append("call_reason: ").append(scenario.goal().callReason()).append(System.lineSeparator());
		prompt.append("expected_risks: ")
				.append(String.join("; ", scenario.conversationQuality().expectedRisks()))
				.append(System.lineSeparator());
		prompt.append(System.lineSeparator());
		prompt.append("Transcript turns").append(System.lineSeparator());
		for (TranscriptTurn turn : transcript.turns()) {
			prompt.append(turn.index())
					.append(". speaker=")
					.append(turn.speaker())
					.append(" timestamp=")
					.append(turn.timestamp())
					.append(" text=")
					.append(turn.text())
					.append(System.lineSeparator());
		}
		return prompt.toString();
	}
}
