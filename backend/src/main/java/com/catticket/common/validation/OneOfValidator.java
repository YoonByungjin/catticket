package com.catticket.common.validation;

import java.util.Set;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class OneOfValidator implements ConstraintValidator<OneOf, String> {

	private Set<String> allowed;

	@Override
	public void initialize(OneOf annotation) {
		this.allowed = Set.of(annotation.value());
	}

	@Override
	public boolean isValid(String value, ConstraintValidatorContext context) {
		return value == null || allowed.contains(value);
	}

}
