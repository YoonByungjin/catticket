package com.catticket.common.validation;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;

/**
 * メールアドレスの形（03 の API-33）。Bean Validation のない場所（B-03）でも API-33 と同じ判定にするため、
 * 同じ制約を付けた入れ物を検証する（判定を書き写すと API とずれるため）。
 */
public final class EmailRule {

	/** 03 の API-33 の形（@Email だけでは「a@b」を通すため、ドメインに「.」を求める） */
	public static final String PATTERN = "^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$";

	public static final int MAX_LENGTH = 254;

	private static final Validator VALIDATOR;

	static {
		// 起動の後に何度も呼ぶので、ファクトリーは1回だけ作る
		@SuppressWarnings("resource")
		ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
		VALIDATOR = factory.getValidator();
	}

	private EmailRule() {
	}

	public static boolean isValid(String email) {
		if (email == null) {
			return false;
		}
		return VALIDATOR.validate(new Holder(email)).isEmpty();
	}

	private record Holder(@Email(message = "FORMAT") @Pattern(regexp = PATTERN, message = "FORMAT")
	@CodePointLength(max = MAX_LENGTH) String email) {
	}

}
