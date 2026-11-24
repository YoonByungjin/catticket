package com.catticket.collect.client.xml;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import tools.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;

/** {@code pblprfr/{mt20id}}（公演詳細）: {@code dbs}/{@code db}（1つ） */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PerformanceDetailResponse(@JacksonXmlElementWrapper(useWrapping = false) @JacksonXmlProperty(
		localName = "db") List<KopisPerformanceDetail> rows) implements KopisEnvelope<KopisPerformanceDetail> {
}
