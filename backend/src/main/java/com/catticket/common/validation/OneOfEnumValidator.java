package com.catticket.common.validation;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class OneOfEnumValidator implements ConstraintValidator<OneOfEnum, String> {

	private Set<String> allowed;

	@Override
	public void initialize(OneOfEnum annotation) {
		this.allowed = Arrays.stream(annotation.value().getEnumConstants())
			.map(Enum::name)
			.collect(Collectors.toUnmodifiableSet());
	}

	@Override
	public boolean isValid(String value, ConstraintValidatorContext context) {
		return value == null || allowed.contains(value);
	}

}
