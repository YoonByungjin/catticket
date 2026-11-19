package com.catticket.surrounding.batch;

import java.util.List;

import com.catticket.common.batch.StepSummary;

/**
 * ファイルから作った入れ替えの内容と件数（06 の 5.4）。
 */
public record SubwayImportPlan(List<StationInsert> stations, List<LockerInsert> lockers, StepSummary summary) {
}
