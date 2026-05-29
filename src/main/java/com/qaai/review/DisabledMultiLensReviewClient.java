package com.qaai.review;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "qaai.review.provider", havingValue = "disabled")
public class DisabledMultiLensReviewClient implements MultiLensReviewClient {

	@Override
	public MultiLensReviewReport review(MultiLensReviewRequest request) {
		throw new MultiLensReviewException("Multi-lens review is disabled by QAAI_REVIEW_PROVIDER=disabled");
	}

	@Override
	public String provider() {
		return "disabled";
	}

	@Override
	public String model() {
		return "none";
	}
}
