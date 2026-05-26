package com.qaai.analysis;

import com.qaai.artifacts.NormalizedTranscript;
import com.qaai.artifacts.TranscriptTurn;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "qaai.analysis.provider", havingValue = "local")
public class LocalAnalysisClient implements AnalysisClient {

	@Override
	public AnalysisReport analyze(AnalysisRequest request) {
		NormalizedTranscript transcript = request.transcript();
		List<TranscriptTurn> turns = transcript.turns() == null ? List.of() : transcript.turns();
		long patientTurns = turns.stream().filter(turn -> "patient".equals(turn.speaker())).count();
		long receptionistTurns = turns.stream().filter(turn -> "receptionist".equals(turn.speaker())).count();

		return new AnalysisReport(
				transcript.callId(),
				request.scenario().id(),
				"Deterministic local analysis reviewed %d transcript turns: %d patient and %d receptionist."
						.formatted(turns.size(), patientTurns, receptionistTurns),
				true,
				List.of(),
				List.of(
						"Local analyzer does not infer bug findings; use it for offline artifact flow checks.",
						"Human review remains required for workflow judgment."
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
}
