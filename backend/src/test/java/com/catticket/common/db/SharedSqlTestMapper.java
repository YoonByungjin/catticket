package com.catticket.common.db;

import java.time.LocalDate;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.catticket.common.display.PerformanceSummaryRow;

/**
 * SharedSql.xml の断片を実際の DB で動かすための、テストだけの Mapper（使う本番の Mapper は Wave 2）。
 * SQL は同じパッケージの SharedSqlTestMapper.xml（src/test/resources/com/catticket/common/db/）。
 * src/test/resources/mapper/ に置くと、mapper-locations（classpath: の後にワイルドカード）は最初に見つかった
 * test の mapper/ だけを探し、main の XML が読まれなくなるため。
 */
@Mapper
public interface SharedSqlTestMapper {

	List<PerformanceSummaryRow> findPublicSummaries(@Param("today") LocalDate today);

}
