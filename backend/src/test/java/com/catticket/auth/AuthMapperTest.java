package com.catticket.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

import com.catticket.auth.row.SessionUserRow;
import com.catticket.common.security.Role;

/**
 * AuthMapper（実際の MySQL のテスト用 DB）。各テストはロールバックされる。
 */
@MybatisTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AuthMapperTest {

	@Autowired
	AuthMapper authMapper;

	@Autowired
	JdbcTemplate jdbcTemplate;

	private long insertUser(String email, String nickname, String role, String status, String hash) {
		KeyHolder keys = new GeneratedKeyHolder();
		jdbcTemplate.update(con -> {
			var ps = con.prepareStatement(
					"INSERT INTO app_user (email, password_hash, nickname, role, status, created_at, updated_at)"
							+ " VALUES (?, ?, ?, ?, ?, '2026-11-10 10:00:00', '2026-11-10 10:00:00')",
					new String[] { "id" });
			ps.setString(1, email);
			ps.setString(2, hash);
			ps.setString(3, nickname);
			ps.setString(4, role);
			ps.setString(5, status);
			return ps;
		}, keys);
		return keys.getKey().longValue();
	}

	@Test
	void findUserByIdReadsSessionColumns() {
		long id = insertUser("cat@example.com", "Cat", "USER", "ACTIVE", "{bcrypt}hash-1");

		Optional<SessionUserRow> row = authMapper.findUserById(id);

		assertThat(row).isPresent();
		assertThat(row.get().id()).isEqualTo(id);
		assertThat(row.get().role()).isEqualTo(Role.USER);
		// DB の ENUM の文字列が UserStatus の名前で対応する
		assertThat(row.get().status()).isEqualTo(UserStatus.ACTIVE);
		assertThat(row.get().isActive()).isTrue();
		assertThat(row.get().nickname()).isEqualTo("Cat");
		assertThat(row.get().passwordHash()).isEqualTo("{bcrypt}hash-1");
	}

	@Test
	void findUserByIdReadsPendingAndAdmin() {
		long pending = insertUser("new@example.com", "New", "USER", "PENDING", "{bcrypt}hash-2");
		long admin = insertUser("admin@example.com", "catticket-admin", "ADMIN", "ACTIVE", "{bcrypt}hash-3");

		assertThat(authMapper.findUserById(pending)).get()
			.extracting(SessionUserRow::status)
			.isEqualTo(UserStatus.PENDING);
		assertThat(authMapper.findUserById(pending)).get().extracting(SessionUserRow::isActive).isEqualTo(false);
		assertThat(authMapper.findUserById(admin)).get().extracting(SessionUserRow::role).isEqualTo(Role.ADMIN);
	}

	@Test
	void findUserByIdIsEmptyWhenMissing() {
		assertThat(authMapper.findUserById(Long.MAX_VALUE)).isEmpty();
	}

	@Test
	void sessionTimeZoneIsSeoul() {
		// U-04: JDBC の URL の connectionTimeZone・forceConnectionTimeZoneToSession（06 の A-05）
		String timeZone = jdbcTemplate.queryForObject("SELECT @@session.time_zone", String.class);
		assertThat(timeZone).isEqualTo("Asia/Seoul");
	}

}
