package com.qaai.artifacts;

public record TranscriptTurn(
		int index,
		String speaker,
		String text,
		Double timestamp
) {
}
