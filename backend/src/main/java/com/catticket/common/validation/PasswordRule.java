package com.catticket.common.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * 新しく設定するパスワードのルール（03 の 4.6）。null・空は通す（必須は @NotNull と組む）。
 */
@Documented
@Constraint(validatedBy = PasswordRuleValidator.class)
@Target({ ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT,
		ElementType.TYPE_USE })
@Retention(RetentionPolicy.RUNTIME)
public @interface PasswordRule {

	String message() default "PASSWORD_RULE";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};

}
