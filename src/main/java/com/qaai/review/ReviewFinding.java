package com.qaai.review;

import java.util.List;

public record ReviewFinding(
		String id,
		String severity,
		String summary,
		List<ReviewEvidenceReference> evidence
) {
}
