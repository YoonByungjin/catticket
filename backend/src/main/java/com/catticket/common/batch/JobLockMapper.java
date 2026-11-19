package com.catticket.common.batch;

import org.apache.ibatis.annotations.Mapper;

/**
 * MySQL の名前付きロック（mapper/common/JobLockMapper.xml）。JobLock が専用の接続（SqlSession）で呼ぶ。
 */
@Mapper
public interface JobLockMapper {

	/** 1 = 取れた、0・null = 取れない（待たない） */
	Integer getLock(String name);

	/** 1 = 外した、0 = 自分のロックではない、null = ロックがない */
	Integer releaseLock(String name);

}
