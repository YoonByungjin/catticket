package com.catticket.collect.client.xml;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import tools.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;

/** {@code pblprfr}（目録）: {@code dbs}/{@code db} */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PerformanceListResponse(@JacksonXmlElementWrapper(useWrapping = false) @JacksonXmlProperty(
		localName = "db") List<KopisPerformanceListItem> rows) implements KopisEnvelope<KopisPerformanceListItem> {
}
