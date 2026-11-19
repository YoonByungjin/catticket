package com.catticket.surrounding.batch;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * SubwayImportMapper（実際の MySQL のテスト用 DB）。各テストはロールバックされる。
 */
@MybatisTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class SubwayImportMapperTest {

	private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 19, 10, 0, 0);

	private static final List<StationInsert> STATIONS = List.of(
			new StationInsert("150", 1, "서울", "ソウルヨク", new BigDecimal("37.55315"), new BigDecimal("126.972533")),
			new StationInsert("243", 2, "충정로", null, new BigDecimal("37.559704"), new BigDecimal("126.964378")));

	private static final List<LockerInsert> LOCKERS = List.of(new LockerInsert("150", "서울역1~22", "1번 출입구"),
			new LockerInsert("150", "서울역23~44", "2,3번 출입구 복도 사이"), new LockerInsert("243", "충정로B", "B 구역"));

	@Autowired
	SubwayImportMapper mapper;

	@Autowired
	JdbcTemplate jdbcTemplate;

	/** SubwayImportWriter.replaceAll と同じ順番 */
	private void replaceAll(LocalDateTime now) {
		mapper.deleteAllLockers();
		mapper.deleteAllStations();
		mapper.insertStations(STATIONS, now);
		mapper.insertLockers(LOCKERS, now);
	}

	private int count(String table) {
		return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
	}

	@Test
	void insertsStationsAndLockersWithStationIdFromCode() {
		replaceAll(NOW);

		Map<String, Object> seoul = jdbcTemplate.queryForMap("SELECT * FROM station WHERE external_station_code = '150'");
		assertThat(((Number) seoul.get("line")).intValue()).isEqualTo(1);
		assertThat(seoul.get("name_ko")).isEqualTo("서울");
		assertThat(seoul.get("name_ja")).isEqualTo("ソウルヨク");
		assertThat(seoul.get("latitude")).isEqualTo(new BigDecimal("37.553150"));
		assertThat(seoul.get("longitude")).isEqualTo(new BigDecimal("126.972533"));
		assertThat(seoul.get("created_at")).isEqualTo(NOW);
		assertThat(seoul.get("updated_at")).isEqualTo(NOW);
		assertThat(jdbcTemplate.queryForObject("SELECT name_ja FROM station WHERE external_station_code = '243'",
				String.class))
			.isNull();

		// 副問い合わせで、外部の駅コードの駅の id になる
		List<Map<String, Object>> lockers = jdbcTemplate.queryForList("SELECT s.external_station_code, l.name,"
				+ " l.location_text, l.created_at FROM coin_locker l JOIN station s ON s.id = l.station_id ORDER BY l.id");
		assertThat(lockers).extracting(r -> r.get("external_station_code")).containsExactly("150", "150", "243");
		assertThat(lockers).extracting(r -> r.get("name")).containsExactly("서울역1~22", "서울역23~44", "충정로B");
		assertThat(lockers.get(1).get("location_text")).isEqualTo("2,3번 출입구 복도 사이");
		assertThat(lockers.get(0).get("created_at")).isEqualTo(NOW);
	}

	@Test
	void replacingTwiceGivesSameRows() {
		replaceAll(NOW);
		replaceAll(NOW.plusDays(90));

		assertThat(count("station")).isEqualTo(2);
		assertThat(count("coin_locker")).isEqualTo(3);
		// 古い駅の id を指すロッカーは残らない
		assertThat(jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM coin_locker l LEFT JOIN station s ON s.id = l.station_id WHERE s.id IS NULL",
				Integer.class))
			.isZero();
		assertThat(jdbcTemplate.queryForObject("SELECT MIN(created_at) FROM station", LocalDateTime.class))
			.isEqualTo(NOW.plusDays(90));
	}

	@Test
	void deleteRemovesExistingRows() {
		replaceAll(NOW);

		assertThat(mapper.deleteAllLockers()).isEqualTo(3);
		assertThat(mapper.deleteAllStations()).isEqualTo(2);
		assertThat(count("station")).isZero();
	}

}
