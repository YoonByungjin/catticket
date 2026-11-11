package com.catticket.common.validation;

import java.util.Optional;
import java.util.regex.Pattern;

import com.catticket.common.error.ApiException;
import com.catticket.common.error.ErrorCode;

/**
 * パスの ID の形（03 の 7.1）。形が合わない ID は 400 にせず、存在しない ID として 404 にする。
 */
public final class PathIds {

	/** KOPIS の ID。長さは 04 の kopis_*_id（VARCHAR(20)） */
	private static final Pattern KOPIS_ID = Pattern.compile("^[A-Za-z0-9]{1,20}$");

	private static final Pattern REVIEW_ID = Pattern.compile("^[1-9][0-9]{0,18}$");

	private PathIds() {
	}

	public static boolean isKopisId(String value) {
		return value != null && KOPIS_ID.matcher(value).matches();
	}

	public static String requirePerformanceId(String value) {
		if (!isKopisId(value)) {
			throw new ApiException(ErrorCode.PERFORMANCE_NOT_FOUND);
		}
		return value;
	}

	public static String requireVenueId(String value) {
		if (!isKopisId(value)) {
			throw new ApiException(ErrorCode.VENUE_NOT_FOUND);
		}
		return value;
	}

	public static Optional<Long> parseReviewId(String value) {
		if (value == null || !REVIEW_ID.matcher(value).matches()) {
			return Optional.empty();
		}
		try {
			return Optional.of(Long.parseLong(value));
		}
		catch (NumberFormatException ex) {
			// 19 桁で Long の最大を超えるもの
			return Optional.empty();
		}
	}

	public static long requireReviewId(String value) {
		return parseReviewId(value).orElseThrow(() -> new ApiException(ErrorCode.REVIEW_NOT_FOUND));
	}

}
