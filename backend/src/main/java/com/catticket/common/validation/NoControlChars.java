package com.catticket.common.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * 制御文字（一般カテゴリ Cc）を含まない。複数行の項目は allowNewlines で LF・CR だけ許す（03 の 7.1）。null は通す。
 */
@Documented
@Constraint(validatedBy = NoControlCharsValidator.class)
@Target({ ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT,
		ElementType.TYPE_USE })
@Retention(RetentionPolicy.RUNTIME)
public @interface NoControlChars {

	boolean allowNewlines() default false;

	String message() default "FORMAT";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};

}
