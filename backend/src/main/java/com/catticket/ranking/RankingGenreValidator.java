package com.catticket.ranking;

import java.util.Arrays;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import com.catticket.common.code.Genre;

public class RankingGenreValidator implements ConstraintValidator<RankingGenre, String> {

	@Override
	public boolean isValid(String value, ConstraintValidatorContext context) {
		return value == null || RankingService.GENRE_ALL.equals(value)
				|| Arrays.stream(Genre.values()).anyMatch(g -> g.code().equals(value));
	}

}
