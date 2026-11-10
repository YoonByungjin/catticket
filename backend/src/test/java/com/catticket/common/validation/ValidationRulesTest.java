package com.catticket.common.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Set;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotNull;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.catticket.common.code.Genre;
import com.catticket.common.code.Lang;
import com.catticket.common.error.ApiException;
import com.catticket.common.error.ErrorCode;

class ValidationRulesTest {

	private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

	record Sample(@CodePointLength(min = 1, max = 3) String name, @NoControlChars String line,
			@NoControlChars(allowNewlines = true) String text, @NotNull(message = "REQUIRED") @PasswordRule String password,
			@OneOf({ "ko", "ja" }) String lang, List<@OneOfEnum(Genre.class) String> genre) {
	}

	private static Set<String> reasons(Sample sample) {
		Set<ConstraintViolation<Sample>> violations = VALIDATOR.validate(sample);
		return violations.stream()
			.map(v -> v.getPropertyPath().toString() + ":" + v.getMessage())
			.collect(java.util.stream.Collectors.toSet());
	}

	@Test
	void validSampleHasNoViolations() {
		assertThat(reasons(new Sample("abc", "one line", "a\nb", "Password1", "ja", List.of("AAAA")))).isEmpty();
		// null は必須以外の制約を通す
		assertThat(reasons(new Sample(null, null, null, "Password1", null, null))).isEmpty();
	}

	@Test
	void eachConstraintUsesReasonCode() {
		Set<String> reasons = reasons(
				new Sample("abcd", "a\nb", "a\tb", "short", "en", List.of("AAAA", "XXXX")));
		assertThat(reasons).contains("name:LENGTH", "line:FORMAT", "text:FORMAT", "password:PASSWORD_RULE",
				"lang:INVALID_VALUE");
		assertThat(reasons).anyMatch(r -> r.startsWith("genre[1]") && r.endsWith(":INVALID_VALUE"));
	}

	@Test
	void emptyPasswordIsLeftToRequired() {
		assertThat(reasons(new Sample(null, null, null, "", null, null))).isEmpty();
		assertThat(reasons(new Sample(null, null, null, null, null, null))).containsExactly("password:REQUIRED");
	}

	@ParameterizedTest
	@ValueSource(strings = { "Password1", "abcdefg1", "A1!\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~" })
	void passwordPolicyAccepts(String password) {
		assertThat(PasswordPolicy.matches(password)).isTrue();
	}

	@ParameterizedTest
	@ValueSource(strings = { "Passwd1", "password", "12345678", "Pass word1", "Password１", "비밀번호abc1",
			"Password1\t" })
	void passwordPolicyRejects(String password) {
		assertThat(PasswordPolicy.matches(password)).isFalse();
	}

	@Test
	void passwordPolicyLengthBounds() {
		assertThat(PasswordPolicy.matches("a1" + "x".repeat(62))).isTrue();
		assertThat(PasswordPolicy.matches("a1" + "x".repeat(63))).isFalse();
		assertThat(PasswordPolicy.matches(null)).isFalse();
	}

	@Test
	void emailRule() {
		assertThat(EmailRule.isValid("cat@example.com")).isTrue();
		assertThat(EmailRule.isValid("cat@example")).isFalse();
		assertThat(EmailRule.isValid("cat example@example.com")).isFalse();
		assertThat(EmailRule.isValid("cat@@example.com")).isFalse();
		assertThat(EmailRule.isValid(null)).isFalse();
		String local = "a".repeat(64);
		String domain = ("b".repeat(60) + ".").repeat(3) + "com"; // 64 + 1 + 186 = 251 文字
		assertThat(EmailRule.isValid(local + "@" + domain)).isTrue();
		assertThat(EmailRule.isValid(local + "@" + "c".repeat(4) + domain)).isFalse();
	}

	@Test
	void pathIds() {
		assertThat(PathIds.isKopisId("PF270001")).isTrue();
		assertThat(PathIds.isKopisId("a".repeat(21))).isFalse();
		assertThat(PathIds.isKopisId("PF-1")).isFalse();
		assertThat(PathIds.requirePerformanceId("PF1")).isEqualTo("PF1");
		assertThatThrownBy(() -> PathIds.requirePerformanceId("../x")).isInstanceOf(ApiException.class)
			.extracting(e -> ((ApiException) e).code())
			.isEqualTo(ErrorCode.PERFORMANCE_NOT_FOUND);
		assertThatThrownBy(() -> PathIds.requireVenueId("")).extracting(e -> ((ApiException) e).code())
			.isEqualTo(ErrorCode.VENUE_NOT_FOUND);

		assertThat(PathIds.parseReviewId("1")).contains(1L);
		assertThat(PathIds.parseReviewId("9223372036854775807")).contains(Long.MAX_VALUE);
		assertThat(PathIds.parseReviewId("9223372036854775808")).isEmpty();
		assertThat(PathIds.parseReviewId("0")).isEmpty();
		assertThat(PathIds.parseReviewId("01")).isEmpty();
		assertThat(PathIds.parseReviewId("-1")).isEmpty();
		assertThatThrownBy(() -> PathIds.requireReviewId("abc")).extracting(e -> ((ApiException) e).code())
			.isEqualTo(ErrorCode.REVIEW_NOT_FOUND);
	}

	@Test
	void pageAndLangQueryDefaults() {
		PageQuery empty = new PageQuery(null, null);
		assertThat(empty.page()).isZero();
		assertThat(empty.size()).isEqualTo(20);
		assertThat(new PageQuery(2, 10).offset()).isEqualTo(20);
		assertThat(new LangQuery(null).toLang()).isEqualTo(Lang.KO);
		assertThat(new LangQuery("ja").toLang()).isEqualTo(Lang.JA);
		assertThat(VALIDATOR.validate(new PageQuery(-1, 101))).hasSize(2)
			.allSatisfy(v -> assertThat(v.getMessage()).isEqualTo("RANGE"));
	}

}
