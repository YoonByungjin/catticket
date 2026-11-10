package com.catticket.auth;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.catticket.auth.row.SessionUserRow;

/**
 * セッションのあるリクエストごとに、利用者の今の行を読む（03 の 7.4、06 の 3.4）。
 */
@Service
public class AuthSessionService {

	private final AuthMapper authMapper;

	public AuthSessionService(AuthMapper authMapper) {
		this.authMapper = authMapper;
	}

	@Transactional(readOnly = true)
	public Optional<SessionUserRow> findSessionUser(long userId) {
		return authMapper.findUserById(userId);
	}

}
