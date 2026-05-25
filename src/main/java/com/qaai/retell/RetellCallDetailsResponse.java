package com.qaai.retell;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RetellCallDetailsResponse(
		@JsonProperty("call_id")
		String callId,
		@JsonProperty("call_status")
		String callStatus,
		@JsonProperty("agent_id")
		String agentId,
		@JsonProperty("from_number")
		String fromNumber,
		@JsonProperty("to_number")
		String toNumber,
		String direction,
		Map<String, String> metadata,
		String transcript,
		@JsonProperty("transcript_object")
		List<TranscriptObjectTurn> transcriptObject,
		@JsonProperty("recording_url")
		String recordingUrl,
		@JsonProperty("duration_ms")
		Long durationMs
) {

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record TranscriptObjectTurn(
			String role,
			String content,
			List<TranscriptWord> words
	) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record TranscriptWord(
			String word,
			Double start,
			Double end
	) {
	}
}
