package com.catticket.collect.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

/**
 * src/test/resources/external/kopis/ のテスト用の応答。
 */
final class KopisFixtures {

	private KopisFixtures() {
	}

	static byte[] bytes(String name) {
		try (InputStream in = KopisFixtures.class.getResourceAsStream("/external/kopis/" + name)) {
			if (in == null) {
				throw new IllegalArgumentException("no fixture: " + name);
			}
			return in.readAllBytes();
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

}
