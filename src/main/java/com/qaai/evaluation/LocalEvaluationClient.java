package com.qaai.evaluation;

import com.qaai.artifacts.NormalizedTranscript;
import com.qaai.artifacts.TranscriptTurn;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "qaai.evaluation.provider", havingValue = "local", matchIfMissing = true)
public class LocalEvaluationClient implements EvaluationClient {

	@Override
	public EvaluationReport evaluate(EvaluationRequest request) {
		NormalizedTranscript transcript = request.transcript();
		List<TranscriptTurn> turns = transcript.turns() == null ? List.of() : transcript.turns();
		TranscriptTurn evidenceTurn = turns.stream()
				.filter(turn -> "receptionist".equals(turn.speaker()))
				.findFirst()
				.orElse(null);

		return new EvaluationReport(
				transcript.callId(),
				request.scenario().id(),
				true,
				EvaluationDimension.supportedValues().stream()
						.map(dimension -> resultFor(dimension, evidenceTurn))
						.toList(),
				List.of(
						"Local evaluator is deterministic and advisory.",
						"Human review owns final QA judgment."
				)
		);
	}

	@Override
	public String provider() {
		return "local";
	}

	@Override
	public String model() {
		return "deterministic-v1";
	}

	private EvaluationDimensionResult resultFor(String dimension, TranscriptTurn evidenceTurn) {
		if (evidenceTurn == null || evidenceTurn.text() == null || evidenceTurn.text().isBlank()) {
			return new EvaluationDimensionResult(
					dimension,
					null,
					"1-5",
					"No receptionist transcript evidence was available for deterministic local evaluation.",
					"high",
					true,
					List.of()
			);
		}

		return new EvaluationDimensionResult(
				dimension,
				3,
				"1-5",
				"Deterministic local evaluation records a neutral advisory score from available transcript evidence.",
				"medium",
				false,
				List.of(new EvaluationEvidenceReference(
						"transcript.txt",
						evidenceTurn.speaker(),
						evidenceTurn.text(),
						evidenceTurn.index()
				))
		);
	}
}
