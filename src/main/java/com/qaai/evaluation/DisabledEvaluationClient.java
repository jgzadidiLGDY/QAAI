package com.qaai.evaluation;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "qaai.evaluation.provider", havingValue = "disabled")
public class DisabledEvaluationClient implements EvaluationClient {

	@Override
	public EvaluationReport evaluate(EvaluationRequest request) {
		throw new EvaluationException("Evaluation is disabled by QAAI_EVALUATOR_PROVIDER=disabled");
	}

	@Override
	public String provider() {
		return "disabled";
	}

	@Override
	public String model() {
		return "none";
	}
}
