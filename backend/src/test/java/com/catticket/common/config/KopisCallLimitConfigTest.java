package com.catticket.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

import com.catticket.common.config.CatticketProperties.Kopis;
import com.catticket.common.config.CatticketProperties.Kopis.CallLimit;

/**
 * KOPIS の上限の設定（06 の 8.1。05 の 3.5）。Spring を起動せずに YAML を読み、プロファイルなしの既定が開発の値であることを固定する。
 */
class KopisCallLimitConfigTest {

	private static List<PropertySource<?>> load(String file) throws IOException {
		return new YamlPropertySourceLoader().load(file, new ClassPathResource(file));
	}

	// 後に足したファイルほど優先する（Spring のプロファイルの重ね方と同じ）
	private static Kopis bind(String... files) throws IOException {
		MutablePropertySources sources = new MutablePropertySources();
		for (String file : files) {
			for (PropertySource<?> source : load(file)) {
				sources.addFirst(source);
			}
		}
		Iterable<ConfigurationPropertySource> attached = ConfigurationPropertySources.from(sources);
		List<ConfigurationPropertySource> list = new ArrayList<>();
		attached.forEach(list::add);
		return new Binder(list).bind("catticket.kopis", Kopis.class).get();
	}

	@Test
	void defaultIsTheDevelopmentLimit() throws IOException {
		Kopis kopis = bind("application.yml");

		assertThat(kopis.interval()).isEqualTo(Duration.ofSeconds(7));
		assertThat(kopis.callLimit()).isEqualTo(new CallLimit(20, 4, 2, 2, 1));
	}

	@Test
	void prodProfileIsTheProductionLimit() throws IOException {
		Kopis kopis = bind("application.yml", "application-prod.yml");

		assertThat(kopis.interval()).isEqualTo(Duration.ofSeconds(7));
		assertThat(kopis.callLimit()).isEqualTo(new CallLimit(300, 70, 10, 30, 5));
		// 本番の設定は上限だけを変える
		assertThat(kopis.baseUrl()).isEqualTo("http://www.kopis.or.kr/openApi/restful");
	}

}
