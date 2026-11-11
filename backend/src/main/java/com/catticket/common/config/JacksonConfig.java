package com.catticket.common.config;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import tools.jackson.databind.cfg.CoercionAction;
import tools.jackson.databind.cfg.CoercionInputShape;
import tools.jackson.databind.cfg.MutableCoercionConfig;
import tools.jackson.databind.type.LogicalType;

/**
 * JSON の型を厳しくする（03 の 7.1、06 の 3.3）。数値の項目への文字列、真偽の項目への文字列・数値を
 * 読めない本文（400 MALFORMED_REQUEST）にする。整数への小数は spring.jackson.deserialization.accept-float-as-int=false。
 */
@Configuration
public class JacksonConfig {

	@Bean
	JsonMapperBuilderCustomizer strictCoercionCustomizer() {
		return builder -> builder.withCoercionConfig(LogicalType.Integer, JacksonConfig::failOnStrings)
			.withCoercionConfig(LogicalType.Float, JacksonConfig::failOnStrings)
			.withCoercionConfig(LogicalType.Boolean,
					c -> failOnStrings(c).setCoercion(CoercionInputShape.Integer, CoercionAction.Fail));
	}

	/**
	 * 文字列を失敗にする。既定では空の文字列（""）は String とは別の形で、黙って null になるので、
	 * EmptyString も失敗にする。空白だけの文字列（" "）も空の扱いにせず、文字列として失敗させる。
	 */
	private static MutableCoercionConfig failOnStrings(MutableCoercionConfig config) {
		return config.setCoercion(CoercionInputShape.String, CoercionAction.Fail)
			.setCoercion(CoercionInputShape.EmptyString, CoercionAction.Fail)
			.setAcceptBlankAsEmpty(false);
	}

}
