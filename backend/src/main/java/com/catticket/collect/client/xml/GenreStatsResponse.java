package com.catticket.collect.client.xml;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import tools.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;

/** {@code boxStatsCate}（ジャンル別統計）: {@code box-statsofs}/{@code boxStatsof} */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GenreStatsResponse(@JacksonXmlElementWrapper(useWrapping = false) @JacksonXmlProperty(
		localName = "boxStatsof") List<KopisGenreStat> rows) implements KopisEnvelope<KopisGenreStat> {
}
