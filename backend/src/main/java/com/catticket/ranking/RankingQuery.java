package com.catticket.ranking;

import java.util.Locale;

import jakarta.validation.constraints.NotNull;

import com.catticket.common.code.Lang;
import com.catticket.common.code.PeriodType;
import com.catticket.common.validation.OneOf;

/**
 * API-20 のクエリ（03 の API-20、06 の 4.4）。genre の省略は ALL。
 * record の部品は受け取った String のまま（LangQuery と同じ。06 の 3.3）なので、型は to… のメソッドで返す。
 */
public record RankingQuery(@NotNull(message = "REQUIRED") @OneOf({ "daily", "weekly", "monthly" }) String period,
		@RankingGenre String genre, @OneOf({ "ko", "ja" }) String lang) {

	public RankingQuery {
		if (genre == null) {
			genre = RankingService.GENRE_ALL;
		}
	}

	/** 検証の後だけ呼ぶ（period は daily / weekly / monthly のどれか） */
	public PeriodType toPeriodType() {
		return PeriodType.valueOf(period.toUpperCase(Locale.ROOT));
	}

	public Lang toLang() {
		return "ja".equals(lang) ? Lang.JA : Lang.KO;
	}

}
