package com.catticket.common.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * enum の定数名のどれか（例: {@code @OneOfEnum(Genre.class)} でジャンルのコード）。null は通す。
 * 繰り返しのクエリは {@code List<@OneOfEnum(Genre.class) String>} のように要素に付ける。
 */
@Documented
@Constraint(validatedBy = OneOfEnumValidator.class)
@Target({ ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT,
		ElementType.TYPE_USE })
@Retention(RetentionPolicy.RUNTIME)
public @interface OneOfEnum {

	Class<? extends Enum<?>> value();

	String message() default "INVALID_VALUE";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};

}
