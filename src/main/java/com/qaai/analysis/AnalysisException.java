package com.qaai.analysis;

public class AnalysisException extends RuntimeException {

	public AnalysisException(String message) {
		super(message);
	}

	public AnalysisException(String message, Throwable cause) {
		super(message, cause);
	}
}
