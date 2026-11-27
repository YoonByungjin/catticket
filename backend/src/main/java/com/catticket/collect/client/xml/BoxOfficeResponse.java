package com.catticket.collect.client.xml;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import tools.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;

/** {@code boxoffice}（順位）: {@code boxofs}/{@code boxof} */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BoxOfficeResponse(@JacksonXmlElementWrapper(useWrapping = false) @JacksonXmlProperty(
		localName = "boxof") List<KopisBoxOfficeItem> rows) implements KopisEnvelope<KopisBoxOfficeItem> {
}
