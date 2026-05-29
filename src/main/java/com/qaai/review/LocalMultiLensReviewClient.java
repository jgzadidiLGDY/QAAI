package com.qaai.review;

import com.qaai.artifacts.TranscriptTurn;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "qaai.review.provider", havingValue = "local", matchIfMissing = true)
public class LocalMultiLensReviewClient implements MultiLensReviewClient {

	@Override
	public MultiLensReviewReport review(MultiLensReviewRequest request) {
		List<TranscriptTurn> turns = request.transcript().turns() == null
				? List.of()
				: request.transcript().turns();
		TranscriptTurn evidenceTurn = turns.stream()
				.filter(turn -> "receptionist".equals(turn.speaker()))
				.findFirst()
				.orElse(null);

		return new MultiLensReviewReport(
				request.transcript().callId(),
				request.scenario().id(),
				request.reviewId(),
				request.generatedAt(),
				provider(),
				model(),
				true,
				ReviewLens.supported().stream()
						.map(lens -> resultFor(lens, evidenceTurn))
						.toList(),
				List.of(
						"Local multi-lens review is deterministic and advisory.",
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

	private ReviewLensResult resultFor(ReviewLens lens, TranscriptTurn evidenceTurn) {
		if (evidenceTurn == null || evidenceTurn.text() == null || evidenceTurn.text().isBlank()) {
			return new ReviewLensResult(
					lens.id(),
					lens.label(),
					"insufficient_evidence",
					"No receptionist transcript evidence was available for deterministic local review.",
					List.of(),
					List.of("Lens could not produce a concrete finding from available transcript evidence.")
			);
		}

		return new ReviewLensResult(
				lens.id(),
				lens.label(),
				"reviewed",
				"Deterministic local review records a neutral advisory finding from available transcript evidence.",
				List.of(new ReviewFinding(
						lens.id() + "_local_observation",
						"info",
						"Review lens observed available receptionist evidence for human inspection.",
						List.of(new ReviewEvidenceReference(
								"transcript.txt",
								evidenceTurn.speaker(),
								evidenceTurn.text(),
								evidenceTurn.index()
						))
				)),
				List.of()
		);
	}
}
