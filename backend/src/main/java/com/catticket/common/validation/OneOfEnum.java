package com.catticket.common.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * enum の値のどれか。null は通す。比べる値は、enum が {@link com.catticket.common.code.ApiCode} を実装していれば
 * {@code apiValue()}（例: {@code SeoulDistrict} は韓国語の名前 {@code 종로구}）、していなければ定数の名前
 * （例: {@code Genre} はコード {@code AAAA}）。
 * 繰り返しのクエリは {@code List<@OneOfEnum(SeoulDistrict.class) String>} のように要素に付ける。
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
