package com.catticket.surrounding.batch;

/**
 * 駅・コインロッカーのファイルの誤り（06 の 5.4）。メッセージは1行で、ファイルの用途・行番号・列の名前・理由を入れる。
 * このメッセージが実行の記録の error_message になる。
 */
public class SubwayFileException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public SubwayFileException(String message) {
		super(message);
	}

	public SubwayFileException(String message, Throwable cause) {
		super(message, cause);
	}

}
