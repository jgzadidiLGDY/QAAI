package com.qaai.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class DotenvLoader {

	private DotenvLoader() {
	}

	public static void load(Path path) {
		if (!Files.exists(path)) {
			return;
		}

		try {
			List<String> lines = Files.readAllLines(path);
			for (String line : lines) {
				loadLine(line);
			}
		} catch (IOException exception) {
			throw new IllegalStateException("Unable to read .env file at " + path, exception);
		}
	}

	private static void loadLine(String line) {
		String trimmed = line.trim();
		if (trimmed.isEmpty() || trimmed.startsWith("#")) {
			return;
		}

		int separator = trimmed.indexOf('=');
		if (separator <= 0) {
			return;
		}

		String name = trimmed.substring(0, separator).trim();
		String value = unquote(trimmed.substring(separator + 1).trim());
		if (System.getenv(name) == null && System.getProperty(name) == null) {
			System.setProperty(name, value);
		}
	}

	private static String unquote(String value) {
		if (value.length() >= 2
				&& ((value.startsWith("\"") && value.endsWith("\""))
				|| (value.startsWith("'") && value.endsWith("'")))) {
			return value.substring(1, value.length() - 1);
		}
		return value;
	}
}
