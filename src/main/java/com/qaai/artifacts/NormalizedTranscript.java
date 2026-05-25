package com.qaai.artifacts;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record NormalizedTranscript(
		@JsonProperty("call_id")
		String callId,
		@JsonProperty("scenario_id")
		String scenarioId,
		String source,
		List<TranscriptTurn> turns
) {
}
