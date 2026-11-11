package com.catticket.common.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * 決まった値の一覧のどれか（大文字・小文字を区別する）。null は通す。
 */
@Documented
@Constraint(validatedBy = OneOfValidator.class)
@Target({ ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT,
		ElementType.TYPE_USE })
@Retention(RetentionPolicy.RUNTIME)
public @interface OneOf {

	String[] value();

	String message() default "INVALID_VALUE";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};

}
