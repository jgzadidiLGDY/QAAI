package com.qaai.reporting;

import java.nio.file.Path;

public record ReportResult(
		String reportId,
		Path reportDirectory,
		Path reportJson,
		Path reportMarkdown,
		Path reportHtml
) {
}
