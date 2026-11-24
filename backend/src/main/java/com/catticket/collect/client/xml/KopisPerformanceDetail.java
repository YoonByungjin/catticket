package com.catticket.collect.client.xml;

import java.util.List;
import java.util.Objects;

import com.catticket.common.text.Texts;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import tools.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;

/**
 * 公演詳細（{@code pblprfr/{mt20id}}）の使う項目（06 の 7.1）。
 * 1つの値の形が悪いだけで読み込みが失敗しないよう、すべて文字列で受ける（変換は使う側）。
 * 値は strip し、空なら null（空白1文字の項目がある）。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record KopisPerformanceDetail(String mt20id, String prfnm, String prfpdfrom, String prfpdto, String prfcast,
		String prfruntime, String prfage, String pcseguidance, String dtguidance, String poster, String area,
		String genrenm, String openrun, String updatedate, String mt10id,
		@JacksonXmlElementWrapper(localName = "relates") @JacksonXmlProperty(
				localName = "relate") List<KopisRelate> relates) {

	// Jackson の XML は包む要素（relates）の名前で値を渡すが、レコードの部品の注釈は構築子の引数を relate にしてしまう。
	// 構築子を明示して引数の名前を relates に合わせる（部品の注釈は明示した構築子の引数には付かない）
	@JsonCreator
	public KopisPerformanceDetail(@JsonProperty("mt20id") String mt20id, @JsonProperty("prfnm") String prfnm,
			@JsonProperty("prfpdfrom") String prfpdfrom, @JsonProperty("prfpdto") String prfpdto,
			@JsonProperty("prfcast") String prfcast, @JsonProperty("prfruntime") String prfruntime,
			@JsonProperty("prfage") String prfage, @JsonProperty("pcseguidance") String pcseguidance,
			@JsonProperty("dtguidance") String dtguidance, @JsonProperty("poster") String poster,
			@JsonProperty("area") String area, @JsonProperty("genrenm") String genrenm,
			@JsonProperty("openrun") String openrun, @JsonProperty("updatedate") String updatedate,
			@JsonProperty("mt10id") String mt10id, @JsonProperty("relates") List<KopisRelate> relates) {
		this.mt20id = Texts.stripToNull(mt20id);
		this.prfnm = Texts.stripToNull(prfnm);
		this.prfpdfrom = Texts.stripToNull(prfpdfrom);
		this.prfpdto = Texts.stripToNull(prfpdto);
		this.prfcast = Texts.stripToNull(prfcast);
		this.prfruntime = Texts.stripToNull(prfruntime);
		this.prfage = Texts.stripToNull(prfage);
		this.pcseguidance = Texts.stripToNull(pcseguidance);
		this.dtguidance = Texts.stripToNull(dtguidance);
		this.poster = Texts.stripToNull(poster);
		this.area = Texts.stripToNull(area);
		this.genrenm = Texts.stripToNull(genrenm);
		this.openrun = Texts.stripToNull(openrun);
		this.updatedate = Texts.stripToNull(updatedate);
		this.mt10id = Texts.stripToNull(mt10id);
		// relates がない・空ならリストを空にする
		this.relates = relates == null ? List.of() : relates.stream().filter(Objects::nonNull).toList();
	}

}
