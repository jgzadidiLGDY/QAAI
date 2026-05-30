package com.qaai.runner;

public record RunContext(
		String agentProfileId,
		String suiteId,
		String suiteRunId
) {
	public static RunContext none() {
		return new RunContext(null, null, null);
	}
}
