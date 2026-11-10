package com.catticket.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.catticket.auth.row.SessionUserRow;
import com.catticket.common.security.Role;

class AuthSessionServiceTest {

	private final AuthMapper authMapper = mock(AuthMapper.class);

	private final AuthSessionService service = new AuthSessionService(authMapper);

	@Test
	void returnsRowFromMapper() {
		SessionUserRow row = new SessionUserRow(1, Role.USER, "ACTIVE", "Cat", "{bcrypt}x");
		given(authMapper.findUserById(1)).willReturn(Optional.of(row));

		assertThat(service.findSessionUser(1)).contains(row);
	}

	@Test
	void returnsEmptyWhenMissing() {
		given(authMapper.findUserById(2)).willReturn(Optional.empty());

		assertThat(service.findSessionUser(2)).isEmpty();
	}

}
