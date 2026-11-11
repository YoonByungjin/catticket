package com.catticket.common.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * 文字数をコードポイントで数える（標準の @Size は UTF-16 の単位で数えるため。03 の 7.1）。null は通す。
 */
@Documented
@Constraint(validatedBy = CodePointLengthValidator.class)
@Target({ ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT,
		ElementType.TYPE_USE })
@Retention(RetentionPolicy.RUNTIME)
public @interface CodePointLength {

	int min() default 0;

	int max() default Integer.MAX_VALUE;

	String message() default "LENGTH";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};

}
