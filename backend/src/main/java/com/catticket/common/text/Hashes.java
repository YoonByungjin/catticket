package com.catticket.common.text;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * SHA-256 の16進（翻訳の source_hash、トークンの token_hash。04）。
 */
public final class Hashes {

	private Hashes() {
	}

	public static String sha256Hex(String value) {
		try {
			byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(digest);
		}
		catch (NoSuchAlgorithmException ex) {
			// SHA-256 はすべての JDK にある
			throw new IllegalStateException(ex);
		}
	}

}
