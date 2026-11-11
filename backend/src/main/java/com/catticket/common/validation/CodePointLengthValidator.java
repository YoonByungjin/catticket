package com.catticket.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import com.catticket.common.text.Texts;

public class CodePointLengthValidator implements ConstraintValidator<CodePointLength, String> {

	private int min;

	private int max;

	@Override
	public void initialize(CodePointLength annotation) {
		this.min = annotation.min();
		this.max = annotation.max();
	}

	@Override
	public boolean isValid(String value, ConstraintValidatorContext context) {
		if (value == null) {
			return true;
		}
		int length = Texts.codePointLength(value);
		return length >= min && length <= max;
	}

}
