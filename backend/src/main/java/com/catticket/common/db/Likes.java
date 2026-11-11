package com.catticket.common.db;

/**
 * LIKE の部分一致の値。SQL 側は ESCAPE に「\」を指定して使う（06 の 3.6）。
 */
public final class Likes {

	private Likes() {
	}

	public static String contains(String keyword) {
		if (keyword == null) {
			return null;
		}
		String escaped = keyword.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
		return "%" + escaped + "%";
	}

}
