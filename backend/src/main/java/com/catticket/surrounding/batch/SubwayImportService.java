package com.catticket.surrounding.batch;

import java.nio.charset.Charset;
import java.time.Clock;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import com.catticket.common.batch.BatchContext;
import com.catticket.common.batch.BatchRunRecorder;
import com.catticket.common.batch.JobResult;
import com.catticket.common.batch.StepResult;
import com.catticket.common.batch.StepRun;
import com.catticket.common.batch.StepSummary;
import com.catticket.common.batch.TaskName;
import com.catticket.common.config.CatticketProperties;
import com.catticket.common.time.DateTimes;

/**
 * B-02 駅・コインロッカーの取り込み（05 の 4、06 の 5.4）。ファイルを読んで変換するまではトランザクションの外で行い、
 * 入れ替えだけを SubwayImportWriter の1つのトランザクションで行う。失敗したら DB は変わらない。
 */
@Service
public class SubwayImportService {

	/** B-02 は1つのステップとして記録し、名前は処理の名前と同じ（06 の 5.2） */
	static final String STEP_NAME = TaskName.IMPORT_SUBWAY.value();

	/** 運営者が 05 の 4.4 の確認をするため、1 件以上なら内容を WARN で出す件数 */
	static final List<String> WARN_DETAILS = List.of(SubwayRowConverter.STATION_NO_ADDRESS,
			SubwayRowConverter.LOCKER_UNMATCHED, SubwayRowConverter.STATION_ADDRESS_BY_NAME,
			SubwayRowConverter.JA_NAME_GARBLED);

	private static final Logger log = LoggerFactory.getLogger(SubwayImportService.class);

	private final BatchRunRecorder recorder;

	private final SubwayImportWriter writer;

	private final ResourceLoader resourceLoader;

	private final CatticketProperties props;

	private final Clock clock;

	private final SubwayFileReader reader = new SubwayFileReader();

	private final SubwayRowConverter converter = new SubwayRowConverter();

	public SubwayImportService(BatchRunRecorder recorder, SubwayImportWriter writer, ResourceLoader resourceLoader,
			CatticketProperties props, Clock clock) {
		this.recorder = recorder;
		this.writer = writer;
		this.resourceLoader = resourceLoader;
		this.props = props;
		this.clock = clock;
	}

	public JobResult importAll(BatchContext ctx) {
		StepRun step = recorder.startStep(ctx.jobRunId(), STEP_NAME, ctx.now());
		StepSummary summary = null;
		try {
			SubwayImportPlan plan = converter.convert(readFiles());
			summary = plan.summary();
			if (plan.stations().isEmpty()) {
				// ファイルの誤りとみなし、入れ替えない（05 の 4.3 の 6.）
				throw new SubwayFileException(SubwayRowConverter.LOCATION_FILE + ": 入れる駅が 0 件");
			}
			writer.replaceAll(plan, ctx.now());
			long processed = summary.get(SubwayRowConverter.STATIONS_SAVED)
					+ summary.get(SubwayRowConverter.LOCKERS_SAVED);
			recorder.finishStep(step, StepResult.success(processed, summary), DateTimes.now(clock));
			logResult(summary);
			return JobResult.SUCCESS;
		}
		catch (RuntimeException e) {
			String message = BatchRunRecorder.describe(e);
			log.error("import-subway failed: {}", message, e);
			recorder.finishStep(step, StepResult.failure(summary, message), DateTimes.now(clock));
			return JobResult.FAILURE;
		}
	}

	private SubwayFiles readFiles() {
		CatticketProperties.Subway subway = props.subway();
		if (subway == null) {
			throw new SubwayFileException("catticket.subway の設定がない");
		}
		Charset charset = charset(subway.charset());
		return new SubwayFiles(
				read(subway.stationLocation(), charset, SubwayRowConverter.LOCATION_FILE,
						SubwayColumns.LOCATION_REQUIRED),
				read(subway.stationAddress(), charset, SubwayRowConverter.ADDRESS_FILE,
						SubwayColumns.ADDRESS_REQUIRED),
				read(subway.locker(), charset, SubwayRowConverter.LOCKER_FILE, SubwayColumns.LOCKER_REQUIRED),
				read(subway.stationNameJa(), charset, SubwayRowConverter.NAME_FILE, SubwayColumns.NAME_REQUIRED));
	}

	private List<CsvRow> read(String location, Charset charset, String label, List<String> required) {
		if (location == null || location.isBlank()) {
			throw new SubwayFileException(label + ": ファイルのパスの設定がない");
		}
		Resource resource = resourceLoader.getResource(location);
		return reader.read(resource, charset, label, required);
	}

	private static Charset charset(String name) {
		try {
			return Charset.forName(name);
		}
		catch (IllegalArgumentException e) {
			// 名前が誤り・null（IllegalCharsetNameException・UnsupportedCharsetException を含む）
			throw new SubwayFileException("catticket.subway.charset: 使えない文字コード（" + name + "）", e);
		}
	}

	private static void logResult(StepSummary summary) {
		log.info("import-subway counts: {}", summary.counts());
		for (String name : WARN_DETAILS) {
			if (summary.get(name) > 0) {
				log.warn("import-subway {} = {}: {}", name, summary.get(name), summary.details(name));
			}
		}
	}

}
