package com.catticket.common.batch;

import java.time.LocalDate;

/**
 * 今回の実行より前の S1 の最も新しい行（06 の 5.2 の findLatestS1Before。05 の 3.3 の 12.）。
 *
 * @param result          {@code SUCCESS}・{@code FAILURE}、終わりが記録されていなければ null
 * @param collectBaseDate その S1 の収集の基準日（V3 より前の行は null）
 */
public record PreviousS1(String result, LocalDate collectBaseDate) {
}
