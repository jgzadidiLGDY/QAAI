package com.qaai.suite;

import java.nio.file.Path;

public record SuiteRunResult(
		String suiteRunId,
		Path suiteDirectory,
		Path suiteSnapshot,
		Path agentProfileSnapshot,
		Path suiteReportJson,
		Path suiteReportMarkdown
) {
}
