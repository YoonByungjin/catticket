package com.catticket.auth;

import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;

import com.catticket.auth.row.SessionUserRow;

/**
 * 認証の Mapper（mapper/auth/AuthMapper.xml）。ほかのメソッドは機能の Issue（06 の 4.5）で足す。
 */
@Mapper
public interface AuthMapper {

	Optional<SessionUserRow> findUserById(long id);

}
