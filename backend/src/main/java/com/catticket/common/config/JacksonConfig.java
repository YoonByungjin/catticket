package com.catticket.common.config;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import tools.jackson.databind.cfg.CoercionAction;
import tools.jackson.databind.cfg.CoercionInputShape;
import tools.jackson.databind.type.LogicalType;

/**
 * JSON の型を厳しくする（03 の 7.1、06 の 3.3）。数値の項目への文字列、真偽の項目への文字列・数値を
 * 読めない本文（400 MALFORMED_REQUEST）にする。整数への小数は spring.jackson.deserialization.accept-float-as-int=false。
 */
@Configuration
public class JacksonConfig {

	@Bean
	JsonMapperBuilderCustomizer strictCoercionCustomizer() {
		return builder -> builder
			.withCoercionConfig(LogicalType.Integer,
					c -> c.setCoercion(CoercionInputShape.String, CoercionAction.Fail))
			.withCoercionConfig(LogicalType.Float, c -> c.setCoercion(CoercionInputShape.String, CoercionAction.Fail))
			.withCoercionConfig(LogicalType.Boolean, c -> c.setCoercion(CoercionInputShape.String, CoercionAction.Fail)
				.setCoercion(CoercionInputShape.Integer, CoercionAction.Fail));
	}

}
