package com.qaai.artifacts;

public class ArtifactWriteException extends RuntimeException {

	public ArtifactWriteException(String message) {
		super(message);
	}

	public ArtifactWriteException(String message, Throwable cause) {
		super(message, cause);
	}
}
