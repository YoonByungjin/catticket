package com.catticket.ranking;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * 順位のジャンルのクエリ: {@code ALL} か {@link com.catticket.common.code.Genre} のコード（03 の API-20）。null は通す。
 * {@code @OneOfEnum(Genre.class)} は ALL を受け付けないので、順位の機能だけの制約にする。
 */
@Documented
@Constraint(validatedBy = RankingGenreValidator.class)
@Target({ ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT })
@Retention(RetentionPolicy.RUNTIME)
public @interface RankingGenre {

	String message() default "INVALID_VALUE";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};

}
