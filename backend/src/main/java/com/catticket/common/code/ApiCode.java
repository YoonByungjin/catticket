package com.catticket.common.code;

/**
 * API で受け取る値が定数の名前と違う enum が実装する（今は {@link SeoulDistrict} だけ）。
 * {@code @OneOfEnum} はこの値で比べる（06 の 3.3、Issue #23 の D-3）。
 */
public interface ApiCode {

	String apiValue();

}
