package com.qaai.quality;

import java.util.List;

public record ConversationDepthReviewResult(
		String durationStatus,
		Long durationSeconds,
		int totalTurns,
		int patientTurns,
		int receptionistTurns,
		boolean patientStatedGoal,
		boolean receptionistAskedWorkflowQuestion,
		boolean reachedConfirmationOrNextStep,
		List<String> markdownLines
) {
}
