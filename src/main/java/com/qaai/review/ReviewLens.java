package com.qaai.review;

import java.util.Arrays;
import java.util.List;

public enum ReviewLens {
	SAFETY("safety", "Safety"),
	CONSISTENCY("consistency", "Consistency"),
	PATIENT_REALISM("patient_realism", "Patient realism"),
	ADVERSARIAL_ROBUSTNESS("adversarial_robustness", "Adversarial robustness"),
	WORKFLOW_RISK("workflow_risk", "Workflow risk");

	private final String id;
	private final String label;

	ReviewLens(String id, String label) {
		this.id = id;
		this.label = label;
	}

	public String id() {
		return id;
	}

	public String label() {
		return label;
	}

	public static List<ReviewLens> supported() {
		return Arrays.asList(values());
	}

	public static List<String> supportedIds() {
		return Arrays.stream(values()).map(ReviewLens::id).toList();
	}
}
