package com.qaai.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DotenvLoaderTest {

	@TempDir
	private Path tempDir;

	@Test
	void loadsDotenvValuesAsSystemPropertiesWithoutOverwritingExistingValues() throws Exception {
		String existingName = "QAAI_DOTENV_TEST_EXISTING";
		String loadedName = "QAAI_DOTENV_TEST_LOADED";
		String quotedName = "QAAI_DOTENV_TEST_QUOTED";
		System.setProperty(existingName, "keep-me");
		System.clearProperty(loadedName);
		System.clearProperty(quotedName);
		Path dotenv = tempDir.resolve(".env");
		Files.writeString(dotenv, """
				# local config
				QAAI_DOTENV_TEST_EXISTING=replace-me
				QAAI_DOTENV_TEST_LOADED=loaded-value
				QAAI_DOTENV_TEST_QUOTED="quoted value"
				""");

		DotenvLoader.load(dotenv);

		assertThat(System.getProperty(existingName)).isEqualTo("keep-me");
		assertThat(System.getProperty(loadedName)).isEqualTo("loaded-value");
		assertThat(System.getProperty(quotedName)).isEqualTo("quoted value");

		System.clearProperty(existingName);
		System.clearProperty(loadedName);
		System.clearProperty(quotedName);
	}
}
