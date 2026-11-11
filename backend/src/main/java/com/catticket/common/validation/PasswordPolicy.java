package com.catticket.common.validation;

/**
 * パスワードのルール（03 の 4.6、01 の R-64）: 英字・数字・記号（0x21〜0x7E）だけで 8〜64 文字、英字と数字を両方含む。
 * ASCII だけなので 64 文字でも 64 バイトで、bcrypt の 72 バイトの制限に当たらない。Spring に依存しない。
 */
public final class PasswordPolicy {

	public static final int MIN_LENGTH = 8;

	public static final int MAX_LENGTH = 64;

	private PasswordPolicy() {
	}

	public static boolean matches(String password) {
		if (password == null || password.length() < MIN_LENGTH || password.length() > MAX_LENGTH) {
			return false;
		}
		boolean hasLetter = false;
		boolean hasDigit = false;
		for (int i = 0; i < password.length(); i++) {
			char c = password.charAt(i);
			// 空白（0x20）は記号に含めない（03 の 4.6 の「記号の範囲」）
			if (c < 0x21 || c > 0x7E) {
				return false;
			}
			if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z')) {
				hasLetter = true;
			}
			else if (c >= '0' && c <= '9') {
				hasDigit = true;
			}
		}
		return hasLetter && hasDigit;
	}

}
