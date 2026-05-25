package com.qaai.analysis;

public record EvidenceReference(
		String artifact,
		String speaker,
		String quote,
		Double timestamp
) {
}
