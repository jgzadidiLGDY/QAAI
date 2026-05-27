package com.qaai.quality;

import com.qaai.artifacts.ArtifactWriter;
import com.qaai.artifacts.NormalizedTranscript;
import com.qaai.artifacts.RunMetadata;
import com.qaai.artifacts.TranscriptTurn;
import com.qaai.scenario.Scenario;
import com.qaai.scenario.ScenarioLoader;
import com.qaai.scenario.ScenarioValidator;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ConversationQualityReviewService {

	private static final int LONG_PATIENT_TURN_WORDS = 25;

	private final ArtifactWriter artifactWriter;
	private final ScenarioLoader scenarioLoader;
	private final ScenarioValidator scenarioValidator;
	private final ConversationDepthReviewService conversationDepthReviewService;

	@Autowired
	public ConversationQualityReviewService(
			ArtifactWriter artifactWriter,
			ScenarioLoader scenarioLoader,
			ScenarioValidator scenarioValidator,
			ConversationDepthReviewService conversationDepthReviewService
	) {
		this.artifactWriter = artifactWriter;
		this.scenarioLoader = scenarioLoader;
		this.scenarioValidator = scenarioValidator;
		this.conversationDepthReviewService = conversationDepthReviewService;
	}

	public ConversationQualityReviewService(
			ArtifactWriter artifactWriter,
			ScenarioLoader scenarioLoader,
			ScenarioValidator scenarioValidator
	) {
		this(artifactWriter, scenarioLoader, scenarioValidator, new ConversationDepthReviewService());
	}

	public ConversationQualityReviewResult review(String callId) {
		if (callId == null || callId.isBlank()) {
			throw new IllegalArgumentException("Provide --call-id=<local_call_id> with --review-conversation");
		}

		RunMetadata metadata = artifactWriter.readMetadata(callId);
		Path runDirectory = artifactWriter.runDirectory(callId);
		Path scenarioPath = runDirectory.resolve("scenario.yaml");
		Scenario scenario = scenarioLoader.load(scenarioPath);
		scenarioValidator.validate(scenario);
		NormalizedTranscript transcript = readTranscriptIfAvailable(callId, runDirectory);
		String observations = buildObservations(metadata, scenario, transcript);

		Path observationsPath = artifactWriter.writeConversationQualityReview(callId, metadata, observations);
		return new ConversationQualityReviewResult(metadata, runDirectory, observationsPath);
	}

	private NormalizedTranscript readTranscriptIfAvailable(String callId, Path runDirectory) {
		if (!Files.exists(runDirectory.resolve("transcript.json"))) {
			return null;
		}
		return artifactWriter.readTranscript(callId);
	}

	private String buildObservations(RunMetadata metadata, Scenario scenario, NormalizedTranscript transcript) {
		StringBuilder observations = new StringBuilder();
		observations.append("# Conversation Quality Observations").append(System.lineSeparator());
		observations.append(System.lineSeparator());
		observations.append("call_id: ").append(metadata.callId()).append(System.lineSeparator());
		observations.append("scenario_id: ").append(metadata.scenarioId()).append(System.lineSeparator());
		observations.append("run_mode: ").append(metadata.runMode()).append(System.lineSeparator());
		observations.append("status: ").append(metadata.status()).append(System.lineSeparator());
		observations.append(System.lineSeparator());

		observations.append("## Scenario Guidance").append(System.lineSeparator());
		observations.append("- Welcome behavior: ").append(scenario.conversationQuality().welcomeBehavior())
				.append(System.lineSeparator());
		observations.append("- Initiative: ").append(scenario.conversationQuality().initiative())
				.append(System.lineSeparator());
		observations.append("- Pacing: ").append(scenario.conversationQuality().pacing()).append(System.lineSeparator());
		observations.append("- Clarification: ").append(scenario.conversationQuality().clarification())
				.append(System.lineSeparator());
		observations.append("- Expected outcome: ").append(scenario.goal().expectedOutcome())
				.append(System.lineSeparator());
		observations.append(System.lineSeparator());

		observations.append("## Transcript Evidence Checklist").append(System.lineSeparator());
		if (transcript == null || transcript.turns().isEmpty()) {
			observations.append("- Transcript evidence: not available; review after `transcript.json` is captured.")
					.append(System.lineSeparator());
			observations.append("- Human reviewer should not infer conversation quality without transcript evidence.")
					.append(System.lineSeparator());
			appendConversationDepth(observations, metadata, scenario, transcript);
			appendReviewerNotes(observations);
			return observations.toString();
		}

		appendWelcome(observations, transcript.turns());
		appendInitiative(observations, transcript.turns());
		appendPacing(observations, transcript.turns());
		appendClarification(observations, transcript.turns());
		appendWorkflowMovement(observations, scenario, transcript.turns());
		appendConversationDepth(observations, metadata, scenario, transcript);
		appendReviewerNotes(observations);
		return observations.toString();
	}

	private void appendConversationDepth(
			StringBuilder observations,
			RunMetadata metadata,
			Scenario scenario,
			NormalizedTranscript transcript
	) {
		observations.append(System.lineSeparator());
		for (String line : conversationDepthReviewService.review(metadata, scenario, transcript).markdownLines()) {
			observations.append(line).append(System.lineSeparator());
		}
	}

	private void appendWelcome(StringBuilder observations, List<TranscriptTurn> turns) {
		observations.append(System.lineSeparator());
		observations.append("## Welcome Behavior").append(System.lineSeparator());
		TranscriptTurn firstPatientTurn = firstTurnBySpeaker(turns, "patient");
		if (firstPatientTurn == null) {
			observations.append("- Evidence: not observed; no patient turn was captured.").append(System.lineSeparator());
			return;
		}
		observations.append("- Evidence: ").append(formatTurn(firstPatientTurn)).append(System.lineSeparator());
		observations.append("- Review note: compare the first patient turn against the scenario welcome guidance.")
				.append(System.lineSeparator());
	}

	private void appendInitiative(StringBuilder observations, List<TranscriptTurn> turns) {
		observations.append(System.lineSeparator());
		observations.append("## Initiative And Over-Sharing").append(System.lineSeparator());
		List<TranscriptTurn> consecutivePatientTurns = consecutivePatientTurns(turns);
		if (consecutivePatientTurns.isEmpty()) {
			observations.append("- Evidence: no consecutive patient turns observed.").append(System.lineSeparator());
		} else {
			for (TranscriptTurn turn : consecutivePatientTurns) {
				observations.append("- Evidence: ").append(formatTurn(turn)).append(System.lineSeparator());
			}
		}
		observations.append("- Review note: verify whether each volunteered detail was useful and scenario-grounded.")
				.append(System.lineSeparator());
	}

	private void appendPacing(StringBuilder observations, List<TranscriptTurn> turns) {
		observations.append(System.lineSeparator());
		observations.append("## Pacing").append(System.lineSeparator());
		List<TranscriptTurn> longPatientTurns = turns.stream()
				.filter(turn -> "patient".equals(turn.speaker()))
				.filter(turn -> wordCount(turn.text()) > LONG_PATIENT_TURN_WORDS)
				.toList();
		if (longPatientTurns.isEmpty()) {
			observations.append("- Evidence: no patient turn exceeded ")
					.append(LONG_PATIENT_TURN_WORDS)
					.append(" words.")
					.append(System.lineSeparator());
		} else {
			for (TranscriptTurn turn : longPatientTurns) {
				observations.append("- Evidence: ").append(formatTurn(turn)).append(System.lineSeparator());
			}
		}
		observations.append("- Review note: confirm patient turns stayed short enough for a voice conversation.")
				.append(System.lineSeparator());
	}

	private void appendClarification(StringBuilder observations, List<TranscriptTurn> turns) {
		observations.append(System.lineSeparator());
		observations.append("## Clarification And Confusion Recovery").append(System.lineSeparator());
		List<TranscriptTurn> clarificationTurns = turns.stream()
				.filter(turn -> "patient".equals(turn.speaker()))
				.filter(turn -> containsAny(turn.text(), "rephrase", "clarify", "mean", "sorry", "understand"))
				.toList();
		if (clarificationTurns.isEmpty()) {
			observations.append("- Evidence: not observed; no patient clarification turn was captured.")
					.append(System.lineSeparator());
		} else {
			for (TranscriptTurn turn : clarificationTurns) {
				observations.append("- Evidence: ").append(formatTurn(turn)).append(System.lineSeparator());
			}
		}
		observations.append("- Review note: if confusion occurred, verify the patient tried to recover without inventing facts.")
				.append(System.lineSeparator());
	}

	private void appendWorkflowMovement(StringBuilder observations, Scenario scenario, List<TranscriptTurn> turns) {
		observations.append(System.lineSeparator());
		observations.append("## Workflow Movement").append(System.lineSeparator());
		observations.append("- Expected outcome: ").append(scenario.goal().expectedOutcome()).append(System.lineSeparator());
		for (TranscriptTurn turn : lastTurns(turns, 2)) {
			observations.append("- Late-call evidence: ").append(formatTurn(turn)).append(System.lineSeparator());
		}
		observations.append("- Review note: decide whether the call moved toward a confirmation, answer, next step, or clear blocker.")
				.append(System.lineSeparator());
	}

	private void appendReviewerNotes(StringBuilder observations) {
		observations.append(System.lineSeparator());
		observations.append("## Human Reviewer Notes").append(System.lineSeparator());
		observations.append("- Pending human review. Do not treat this artifact as an automated pass/fail decision.")
				.append(System.lineSeparator());
	}

	private TranscriptTurn firstTurnBySpeaker(List<TranscriptTurn> turns, String speaker) {
		return turns.stream()
				.filter(turn -> speaker.equals(turn.speaker()))
				.findFirst()
				.orElse(null);
	}

	private List<TranscriptTurn> consecutivePatientTurns(List<TranscriptTurn> turns) {
		return turns.stream()
				.filter(turn -> "patient".equals(turn.speaker()))
				.filter(turn -> {
					int turnPosition = turns.indexOf(turn);
					return turnPosition > 0 && "patient".equals(turns.get(turnPosition - 1).speaker());
				})
				.toList();
	}

	private List<TranscriptTurn> lastTurns(List<TranscriptTurn> turns, int count) {
		if (turns.size() <= count) {
			return turns;
		}
		return turns.subList(turns.size() - count, turns.size());
	}

	private String formatTurn(TranscriptTurn turn) {
		return "turn " + turn.index() + " [" + turn.speaker() + "] " + turn.text();
	}

	private int wordCount(String text) {
		if (text == null || text.isBlank()) {
			return 0;
		}
		return text.trim().split("\\s+").length;
	}

	private boolean containsAny(String text, String... values) {
		if (text == null) {
			return false;
		}
		String lowerText = text.toLowerCase();
		for (String value : values) {
			if (lowerText.contains(value)) {
				return true;
			}
		}
		return false;
	}
}
