package com.catticket.common.validation;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import com.catticket.common.code.ApiCode;

public class OneOfEnumValidator implements ConstraintValidator<OneOfEnum, String> {

	private Set<String> allowed;

	@Override
	public void initialize(OneOfEnum annotation) {
		this.allowed = Arrays.stream(annotation.value().getEnumConstants())
			.map(OneOfEnumValidator::valueOf)
			.collect(Collectors.toUnmodifiableSet());
	}

	// ApiCode を実装する enum は API の値、ほかは定数の名前で比べる（06 の 3.3）
	private static String valueOf(Enum<?> constant) {
		return constant instanceof ApiCode code ? code.apiValue() : constant.name();
	}

	@Override
	public boolean isValid(String value, ConstraintValidatorContext context) {
		return value == null || allowed.contains(value);
	}

}
