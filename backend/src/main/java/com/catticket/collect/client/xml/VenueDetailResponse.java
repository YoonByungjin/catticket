package com.catticket.collect.client.xml;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import tools.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;

/** {@code prfplc/{mt10id}}（施設詳細）: {@code dbs}/{@code db}（1つ） */
@JsonIgnoreProperties(ignoreUnknown = true)
public record VenueDetailResponse(@JacksonXmlElementWrapper(useWrapping = false) @JacksonXmlProperty(
		localName = "db") List<KopisVenueDetail> rows) implements KopisEnvelope<KopisVenueDetail> {
}
