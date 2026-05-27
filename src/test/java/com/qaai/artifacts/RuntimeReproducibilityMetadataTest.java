package com.qaai.artifacts;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class RuntimeReproducibilityMetadataTest {

	@AfterEach
	void clearProperties() {
		System.clearProperty("QAAI_APP_VERSION");
	}

	@Test
	void readsAppVersionFromSystemPropertyLoadedByDotenv() {
		System.setProperty("QAAI_APP_VERSION", "phase-12-test");

		ReproducibilityMetadata metadata = RuntimeReproducibilityMetadata.forCommand("dry-run");

		assertThat(metadata.command()).isEqualTo("dry-run");
		assertThat(metadata.appVersion()).isEqualTo("phase-12-test");
	}
}
