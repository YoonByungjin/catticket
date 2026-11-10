package com.catticket.common.db;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.SQLException;

import org.junit.jupiter.api.Test;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.UncategorizedSQLException;

class DbHelpersTest {

	@Test
	void likesEscapesWildcards() {
		assertThat(Likes.contains("cat")).isEqualTo("%cat%");
		assertThat(Likes.contains("50%_\\")).isEqualTo("%50\\%\\_\\\\%");
		assertThat(Likes.contains(null)).isNull();
	}

	@Test
	void isDuplicateKeyMatchesConstraintName() {
		SQLException cause = new SQLException(
				"Duplicate entry 'a@example.com' for key 'app_user.uk_app_user_email'", "23000", 1062);
		DuplicateKeyException ex = new DuplicateKeyException("insert failed", cause);

		assertThat(DbErrors.isDuplicateKey(ex, "uk_app_user_email")).isTrue();
		assertThat(DbErrors.isDuplicateKey(ex, "uk_app_user_active_nickname")).isFalse();
		// 名前の一部だけが一致するものは別の制約
		assertThat(DbErrors.isDuplicateKey(ex, "app_user_email")).isFalse();
		assertThat(DbErrors.isDuplicateKey(new DataIntegrityViolationException("x", cause), "uk_app_user_email"))
			.isFalse();
	}

	@Test
	void isLockFailure() {
		assertThat(DbErrors.isLockFailure(new CannotAcquireLockException("deadlock"))).isTrue();
		assertThat(DbErrors.isLockFailure(
				new UncategorizedSQLException("x", "sql", new SQLException("Lock wait timeout", "HY000", 1205))))
			.isTrue();
		assertThat(DbErrors.isLockFailure(
				new UncategorizedSQLException("x", "sql", new SQLException("other", "HY000", 1064))))
			.isFalse();
	}

}
