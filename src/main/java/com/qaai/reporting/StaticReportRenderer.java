package com.qaai.reporting;

import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class StaticReportRenderer {

	public String renderMarkdown(ReportModel report) {
		StringBuilder markdown = new StringBuilder();
		markdown.append("# QA Static Report%n%n".formatted());
		markdown.append("report_id: ").append(report.reportId()).append("%n".formatted());
		markdown.append("generated_at: ").append(report.generatedAt()).append("%n".formatted());
		markdown.append("human_review_required: ").append(report.humanReviewRequired()).append("%n%n".formatted());
		markdown.append("This report summarizes existing artifacts only. It does not create pass/fail decisions.%n%n"
				.formatted());

		markdown.append("## Run History%n%n".formatted());
		if (report.runs().isEmpty()) {
			markdown.append("No runs found.%n%n".formatted());
		} else {
			markdown.append("| Call ID | Scenario | Mode | Status | Complete | Artifacts |%n".formatted());
			markdown.append("| --- | --- | --- | --- | --- | --- |%n".formatted());
			for (ReportRunSummary run : report.runs()) {
				markdown.append("| ")
						.append(escapeTable(run.callId())).append(" | ")
						.append(escapeTable(run.scenarioId())).append(" | ")
						.append(escapeTable(run.runMode())).append(" | ")
						.append(escapeTable(run.status())).append(" | ")
						.append(run.complete()).append(" | ")
						.append(artifactLinks(run)).append(" |%n".formatted());
			}
			markdown.append("%n".formatted());
		}

		markdown.append("## Evaluation Scores%n%n".formatted());
		if (report.evaluationScores().isEmpty()) {
			markdown.append("No evaluation artifacts found.%n%n".formatted());
		} else {
			markdown.append("| Dimension | Scored | Average | Insufficient Evidence |%n".formatted());
			markdown.append("| --- | ---: | ---: | ---: |%n".formatted());
			for (ReportEvaluationSummary summary : report.evaluationScores().values()) {
				markdown.append("| ")
						.append(escapeTable(summary.dimension())).append(" | ")
						.append(summary.scoredCount()).append(" | ")
						.append(summary.averageScore() == null ? "n/a" : "%.2f".formatted(summary.averageScore()))
						.append(" | ")
						.append(summary.insufficientEvidenceCount()).append(" |%n".formatted());
			}
			markdown.append("%n".formatted());
		}

		markdown.append("## Bug Severity%n%n".formatted());
		if (report.severityCounts().isEmpty()) {
			markdown.append("No analysis findings found.%n%n".formatted());
		} else {
			for (Map.Entry<String, Long> entry : report.severityCounts().entrySet()) {
				markdown.append("- ").append(entry.getKey()).append(": ").append(entry.getValue()).append("%n"
						.formatted());
			}
			markdown.append("%n".formatted());
		}

		markdown.append("## Scenario Coverage%n%n".formatted());
		if (report.coverage().isEmpty()) {
			markdown.append("No scenario coverage metadata found.%n".formatted());
		} else {
			markdown.append("| Scenario | Workflow Area | Edge Cases | Risk Focus |%n".formatted());
			markdown.append("| --- | --- | --- | --- |%n".formatted());
			for (ReportScenarioCoverageSummary coverage : report.coverage()) {
				markdown.append("| ")
						.append(escapeTable(coverage.scenarioId())).append(" | ")
						.append(escapeTable(coverage.workflowArea())).append(" | ")
						.append(escapeTable(String.join(", ", coverage.edgeCases()))).append(" | ")
						.append(escapeTable(coverage.riskFocus())).append(" |%n".formatted());
			}
		}
		return markdown.toString();
	}

	public String renderHtml(ReportModel report) {
		String markdown = renderMarkdown(report);
		StringBuilder html = new StringBuilder();
		html.append("<!doctype html>%n<html lang=\"en\">%n<head>%n".formatted());
		html.append("<meta charset=\"utf-8\">%n".formatted());
		html.append("<title>QA Static Report</title>%n".formatted());
		html.append("<style>%n".formatted());
		html.append("body{font-family:Arial,sans-serif;line-height:1.5;margin:2rem;max-width:1100px;}%n"
				.formatted());
		html.append("pre{white-space:pre-wrap;background:#f6f8fa;padding:1rem;border-radius:6px;}%n"
				.formatted());
		html.append("</style>%n</head>%n<body>%n".formatted());
		html.append("<pre>").append(escapeHtml(markdown)).append("</pre>%n".formatted());
		html.append("</body>%n</html>%n".formatted());
		return html.toString();
	}

	private String artifactLinks(ReportRunSummary run) {
		StringBuilder links = new StringBuilder();
		appendLink(links, "metadata", run.metadataPath());
		appendLink(links, "transcript", run.transcriptPath());
		appendLink(links, "analysis", run.analysisPath());
		appendLink(links, "evaluation", run.evaluationPath());
		if (links.isEmpty()) {
			return "none";
		}
		return links.toString();
	}

	private void appendLink(StringBuilder links, String label, String path) {
		if (path == null || path.isBlank()) {
			return;
		}
		if (!links.isEmpty()) {
			links.append(", ");
		}
		links.append("[").append(label).append("](").append(path.replace("\\", "/")).append(")");
	}

	private String escapeTable(String value) {
		if (value == null) {
			return "";
		}
		return value.replace("|", "\\|").replace("%n".formatted(), " ");
	}

	private String escapeHtml(String value) {
		return value
				.replace("&", "&amp;")
				.replace("<", "&lt;")
				.replace(">", "&gt;");
	}
}
