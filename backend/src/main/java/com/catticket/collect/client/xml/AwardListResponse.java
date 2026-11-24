package com.catticket.collect.client.xml;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import tools.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;

/** {@code prfawad}（受賞作）: {@code dbs}/{@code db} */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AwardListResponse(@JacksonXmlElementWrapper(useWrapping = false) @JacksonXmlProperty(
		localName = "db") List<KopisAwardItem> rows) implements KopisEnvelope<KopisAwardItem> {
}
