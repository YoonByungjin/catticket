package com.catticket.ranking;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.catticket.ranking.dto.RankingResponse;

/**
 * 人気順位の API（06 の 4.4）。急上昇（API-21）・ジャンル別トレンド（API-22）は別の Issue で足す。
 */
@RestController
@RequestMapping("/api")
public class RankingController {

	private final RankingService rankingService;

	public RankingController(RankingService rankingService) {
		this.rankingService = rankingService;
	}

	/** API-20 */
	@GetMapping("/rankings")
	public RankingResponse rankings(@Valid RankingQuery query) {
		return rankingService.getRanking(query.toPeriodType(), query.genre(), query.toLang());
	}

}
