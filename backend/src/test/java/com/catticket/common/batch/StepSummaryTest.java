package com.catticket.common.batch;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.json.JsonMapper;

/**
 * StepSummary（件数・内容と JSON の形。06 の 5.2）。
 */
class StepSummaryTest {

	private final JsonMapper jsonMapper = JsonMapper.builder().build();

	@Test
	void countsKeepInsertionOrderAndDefaultToZero() {
		StepSummary s = new StepSummary();
		s.increment("saved");
		s.increment("saved");
		s.add("skippedGenre", 2);
		s.add("zero", 0);

		assertThat(s.get("saved")).isEqualTo(2);
		assertThat(s.get("missing")).isZero();
		assertThat(s.counts().keySet()).containsExactly("saved", "skippedGenre", "zero");
		assertThat(jsonMapper.writeValueAsString(s)).isEqualTo("{\"saved\":2,\"skippedGenre\":2,\"zero\":0}");
	}

	@Test
	void detailsAreUnderDetailsAndCapped() {
		StepSummary s = new StepSummary();
		for (int i = 0; i < 60; i++) {
			s.increment("lockerUnmatched");
			s.addDetail("lockerUnmatched", "row" + i);
		}

		assertThat(s.get("lockerUnmatched")).isEqualTo(60);
		assertThat(s.details("lockerUnmatched")).hasSize(StepSummary.MAX_DETAILS).startsWith("row0");
		assertThat(jsonMapper.writeValueAsString(s)).startsWith("{\"lockerUnmatched\":60,\"details\":{\"lockerUnmatched\":[\"row0\",");
	}

}
