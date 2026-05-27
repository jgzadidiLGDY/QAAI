package com.qaai.artifacts;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class RuntimeReproducibilityMetadata {

	private static final String DEFAULT_APP_VERSION = "0.0.1-SNAPSHOT";

	private RuntimeReproducibilityMetadata() {
	}

	public static ReproducibilityMetadata forCommand(String command) {
		return new ReproducibilityMetadata(command, appVersion(), gitCommit());
	}

	private static String appVersion() {
		String configuredVersion = System.getenv("QAAI_APP_VERSION");
		if (configuredVersion != null && !configuredVersion.isBlank()) {
			return configuredVersion;
		}
		String implementationVersion = RuntimeReproducibilityMetadata.class.getPackage().getImplementationVersion();
		if (implementationVersion != null && !implementationVersion.isBlank()) {
			return implementationVersion;
		}
		return DEFAULT_APP_VERSION;
	}

	private static String gitCommit() {
		Path gitDirectory = Path.of(".git");
		Path headPath = gitDirectory.resolve("HEAD");
		if (!Files.exists(headPath)) {
			return null;
		}
		try {
			String head = Files.readString(headPath).trim();
			if (head.startsWith("ref: ")) {
				Path refPath = gitDirectory.resolve(head.substring("ref: ".length()));
				if (Files.exists(refPath)) {
					return blankToNull(Files.readString(refPath).trim());
				}
				return null;
			}
			return blankToNull(head);
		} catch (IOException exception) {
			return null;
		}
	}

	private static String blankToNull(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return value;
	}
}
