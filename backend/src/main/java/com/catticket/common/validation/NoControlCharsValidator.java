package com.catticket.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import com.catticket.common.text.Texts;

public class NoControlCharsValidator implements ConstraintValidator<NoControlChars, String> {

	private boolean allowNewlines;

	@Override
	public void initialize(NoControlChars annotation) {
		this.allowNewlines = annotation.allowNewlines();
	}

	@Override
	public boolean isValid(String value, ConstraintValidatorContext context) {
		return !Texts.hasControlChars(value, allowNewlines);
	}

}
