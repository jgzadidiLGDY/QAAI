package com.qaai.runner;

record AudioCapture(
		byte[] bytes,
		String note
) {

	boolean present() {
		return bytes != null && bytes.length > 0;
	}
}
