package com.catticket.common.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * 役割（04 の app_user.role）。Spring Security では ROLE_USER・ROLE_ADMIN の権限にする。
 */
public enum Role {

	USER, ADMIN;

	public GrantedAuthority authority() {
		return new SimpleGrantedAuthority("ROLE_" + name());
	}

}
