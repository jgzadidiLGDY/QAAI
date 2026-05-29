package com.qaai.review;

public interface MultiLensReviewClient {

	MultiLensReviewReport review(MultiLensReviewRequest request);

	default String provider() {
		return "unknown";
	}

	default String model() {
		return null;
	}
}
