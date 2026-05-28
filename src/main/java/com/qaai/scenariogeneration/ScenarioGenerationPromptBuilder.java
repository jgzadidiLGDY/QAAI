package com.qaai.scenariogeneration;

import org.springframework.stereotype.Component;

@Component
public class ScenarioGenerationPromptBuilder {

	public String build(String agentDescription, int scenarioCount) {
		return """
				Generate a bounded draft scenario library for this voice agent under test:

				%s

				Return JSON only with this shape:
				{
				  "coverage_plan_markdown": "Markdown coverage plan for human review",
				  "scenarios": [
				    {
				      "id": "snake_case_unique_id",
				      "name": "Human readable name",
				      "workflow": "workflow_area",
				      "persona": {
				        "name": "Synthetic patient name",
				        "date_of_birth": "YYYY-MM-DD",
				        "phone_number": "+15555550100"
				      },
				      "goal": {
				        "call_reason": "short patient-facing reason",
				        "summary": "scenario goal",
				        "expected_outcome": "expected target-side outcome"
				      },
				      "constraints": {
				        "allowed_facts": ["synthetic fact"],
				        "disallowed_behavior": ["Do not provide real patient data."]
				      },
				      "coverage": {
				        "workflow_area": "workflow_area",
				        "edge_cases": ["happy_path"],
				        "risk_focus": "review risk"
				      },
				      "conversation_quality": {
				        "welcome_behavior": "how the patient starts",
				        "initiative": "how much the patient volunteers",
				        "pacing": "turn-taking guidance",
				        "clarification": "how to recover from confusion",
				        "expected_risks": ["risk to review"]
				      },
				      "steps": [
				        {
				          "intent": "greeting",
				          "patient_says": "Hi, I need help."
				        }
				      ]
				    }
				  ]
				}

				Generate exactly %d scenarios. Use only synthetic patients and facts.
				Allowed edge_cases values are: happy_path, missing_fact, clarification,
				transfer_or_hold, ambiguous_next_step, unavailable_information,
				workflow_recovery, workflow_mismatch.
				Drafts are review artifacts. Do not claim complete behavioral coverage.
				""".formatted(agentDescription, scenarioCount);
	}
}
