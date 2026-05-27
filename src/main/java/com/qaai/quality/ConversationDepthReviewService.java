package com.qaai.quality;

import com.qaai.artifacts.NormalizedTranscript;
import com.qaai.artifacts.RunMetadata;
import com.qaai.artifacts.TranscriptTurn;
import com.qaai.scenario.Scenario;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class ConversationDepthReviewService {

	private static final int SHORT_CALL_SECONDS = 60;
	private static final int TYPICAL_CALL_SECONDS = 240;
	private static final int MIN_TOTAL_TURNS = 6;
	private static final int MIN_TURNS_PER_SIDE = 2;

	public ConversationDepthReviewResult review(
			RunMetadata metadata,
			Scenario scenario,
			NormalizedTranscript transcript
	) {
		List<TranscriptTurn> turns = transcript == null ? List.of() : transcript.turns();
		Long durationSeconds = durationSeconds(metadata, turns);
		String durationStatus = durationStatus(durationSeconds);
		int patientTurns = countTurns(turns, "patient");
		int receptionistTurns = countTurns(turns, "receptionist");
		boolean patientStatedGoal = patientStatedGoal(scenario, turns);
		boolean receptionistAskedWorkflowQuestion = receptionistAskedWorkflowQuestion(scenario, turns);
		boolean reachedConfirmationOrNextStep = reachedConfirmationOrNextStep(turns);

		List<String> lines = new ArrayList<>();
		lines.add("## Conversation Depth Signals");
		lines.add("- Duration: " + durationLabel(durationStatus, durationSeconds));
		lines.add("- Turn count: " + turns.size() + " total, " + patientTurns + " patient, "
				+ receptionistTurns + " receptionist.");
		lines.add("- Depth concern: " + depthConcern(turns.size(), patientTurns, receptionistTurns));
		lines.add("- Patient stated goal: " + yesNo(patientStatedGoal, firstGoalEvidence(scenario, turns)));
		lines.add("- Receptionist asked workflow-specific question: "
				+ yesNo(receptionistAskedWorkflowQuestion, firstWorkflowQuestionEvidence(scenario, turns)));
		lines.add("- Confirmation or next step reached: "
				+ yesNo(reachedConfirmationOrNextStep, firstNextStepEvidence(turns)));
		lines.add("- Review note: these are advisory depth signals for human review, not automated pass/fail.");

		return new ConversationDepthReviewResult(
				durationStatus,
				durationSeconds,
				turns.size(),
				patientTurns,
				receptionistTurns,
				patientStatedGoal,
				receptionistAskedWorkflowQuestion,
				reachedConfirmationOrNextStep,
				lines
		);
	}

	private Long durationSeconds(RunMetadata metadata, List<TranscriptTurn> turns) {
		if (metadata.callDurationSeconds() != null) {
			return metadata.callDurationSeconds();
		}
		return turns.stream()
				.map(TranscriptTurn::timestamp)
				.filter(timestamp -> timestamp != null && timestamp >= 0)
				.max(Double::compareTo)
				.map(timestamp -> Math.round(Math.ceil(timestamp)))
				.orElse(null);
	}

	private String durationStatus(Long durationSeconds) {
		if (durationSeconds == null) {
			return "unknown";
		}
		if (durationSeconds < SHORT_CALL_SECONDS) {
			return "short";
		}
		if (durationSeconds <= TYPICAL_CALL_SECONDS) {
			return "typical";
		}
		return "long";
	}

	private String durationLabel(String durationStatus, Long durationSeconds) {
		if (durationSeconds == null) {
			return "unknown; no captured duration or transcript timestamps were available.";
		}
		String label = durationSeconds + " seconds (" + durationStatus + ").";
		if ("short".equals(durationStatus)) {
			return label + " Typical medical appointment calls are expected to run about 1 to 4 minutes.";
		}
		if ("typical".equals(durationStatus)) {
			return label + " This is within the 1 to 4 minute review band for a typical appointment call.";
		}
		return label + " Review whether the extra length reflects useful workflow work or drift.";
	}

	private int countTurns(List<TranscriptTurn> turns, String speaker) {
		return (int) turns.stream()
				.filter(turn -> speaker.equals(turn.speaker()))
				.count();
	}

	private String depthConcern(int totalTurns, int patientTurns, int receptionistTurns) {
		if (totalTurns < MIN_TOTAL_TURNS) {
			return "conversation has fewer than " + MIN_TOTAL_TURNS + " captured turns.";
		}
		if (patientTurns < MIN_TURNS_PER_SIDE || receptionistTurns < MIN_TURNS_PER_SIDE) {
			return "one side has fewer than " + MIN_TURNS_PER_SIDE + " captured turns.";
		}
		return "not observed from turn counts.";
	}

	private boolean patientStatedGoal(Scenario scenario, List<TranscriptTurn> turns) {
		return firstGoalEvidence(scenario, turns) != null;
	}

	private String firstGoalEvidence(Scenario scenario, List<TranscriptTurn> turns) {
		Set<String> goalTokens = scenarioTokens(
				scenario.workflow(),
				scenario.goal().callReason(),
				scenario.goal().summary()
		);
		return turns.stream()
				.filter(turn -> "patient".equals(turn.speaker()))
				.filter(turn -> tokenMatches(turn.text(), goalTokens, 2))
				.findFirst()
				.map(this::formatTurn)
				.orElse(null);
	}

	private boolean receptionistAskedWorkflowQuestion(Scenario scenario, List<TranscriptTurn> turns) {
		return firstWorkflowQuestionEvidence(scenario, turns) != null;
	}

	private String firstWorkflowQuestionEvidence(Scenario scenario, List<TranscriptTurn> turns) {
		Set<String> workflowTokens = scenarioTokens(
				scenario.workflow(),
				scenario.goal().callReason(),
				scenario.goal().expectedOutcome()
		);
		return turns.stream()
				.filter(turn -> "receptionist".equals(turn.speaker()))
				.filter(turn -> looksLikeQuestion(turn.text()))
				.filter(turn -> tokenMatches(turn.text(), workflowTokens, 1)
						|| containsAny(turn.text(), "date of birth", "dob", "insurance", "appointment", "schedule",
								"prescription", "billing", "callback", "phone number"))
				.findFirst()
				.map(this::formatTurn)
				.orElse(null);
	}

	private boolean reachedConfirmationOrNextStep(List<TranscriptTurn> turns) {
		return firstNextStepEvidence(turns) != null;
	}

	private String firstNextStepEvidence(List<TranscriptTurn> turns) {
		return turns.stream()
				.filter(turn -> containsAnyPhrase(turn.text(), "confirm", "confirmed", "scheduled", "appointment is",
						"next step", "call back", "follow up", "send", "sent", "transfer", "hold", "available",
						"offer", "ready"))
				.findFirst()
				.map(this::formatTurn)
				.orElse(null);
	}

	private boolean looksLikeQuestion(String text) {
		return containsAny(text, "?", "what", "when", "where", "can you", "could you", "do you", "may i",
				"would you", "which");
	}

	private Set<String> scenarioTokens(String... values) {
		Set<String> tokens = new LinkedHashSet<>();
		for (String value : values) {
			if (value == null) {
				continue;
			}
			for (String rawToken : value.toLowerCase(Locale.ROOT).split("[^a-z0-9]+")) {
				String token = normalizeToken(rawToken);
				if (token.length() >= 4 && !stopWords().contains(token)) {
					tokens.add(token);
				}
			}
		}
		return tokens;
	}

	private boolean tokenMatches(String text, Set<String> expectedTokens, int minimumMatches) {
		if (text == null || expectedTokens.isEmpty()) {
			return false;
		}
		Set<String> actualTokens = scenarioTokens(text);
		int matches = 0;
		for (String expectedToken : expectedTokens) {
			for (String actualToken : actualTokens) {
				if (actualToken.contains(expectedToken) || expectedToken.contains(actualToken)) {
					matches++;
					break;
				}
			}
			if (matches >= minimumMatches) {
				return true;
			}
		}
		return false;
	}

	private Set<String> stopWords() {
		return Set.of("with", "that", "this", "from", "have", "need", "about", "existing", "patient");
	}

	private String normalizeToken(String token) {
		if (token.endsWith("ing") && token.length() > 6) {
			return token.substring(0, token.length() - 3);
		}
		if (token.endsWith("ed") && token.length() > 5) {
			return token.substring(0, token.length() - 2);
		}
		if (token.endsWith("s") && token.length() > 5) {
			return token.substring(0, token.length() - 1);
		}
		return token;
	}

	private String yesNo(boolean value, String evidence) {
		if (!value) {
			return "not observed.";
		}
		return "observed; " + evidence;
	}

	private String formatTurn(TranscriptTurn turn) {
		return "turn " + turn.index() + " [" + turn.speaker() + "] " + turn.text();
	}

	private boolean containsAny(String text, String... values) {
		if (text == null) {
			return false;
		}
		String lowerText = text.toLowerCase(Locale.ROOT);
		for (String value : values) {
			if (lowerText.contains(value)) {
				return true;
			}
		}
		return false;
	}

	private boolean containsAnyPhrase(String text, String... values) {
		if (text == null) {
			return false;
		}
		String lowerText = text.toLowerCase(Locale.ROOT);
		for (String value : values) {
			String lowerValue = value.toLowerCase(Locale.ROOT);
			if (Pattern.compile("(^|[^a-z0-9])" + Pattern.quote(lowerValue) + "([^a-z0-9]|$)")
					.matcher(lowerText)
					.find()) {
				return true;
			}
		}
		return false;
	}
}
