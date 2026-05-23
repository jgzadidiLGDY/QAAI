package com.qaai.retell;

public class RetellApiException extends RuntimeException {

	public RetellApiException(String message) {
		super(message);
	}

	public RetellApiException(String message, Throwable cause) {
		super(message, cause);
	}
}
