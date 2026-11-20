package com.catticket.common.batch;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.catticket.common.text.Texts;

import tools.jackson.databind.json.JsonMapper;

/**
 * 実行の記録（05 の 1.1、06 の 5.2）。すべて REQUIRES_NEW（ステップの失敗のロールバックで記録が消えないように）。
 */
@Component
public class BatchRunRecorder {

	/** error_message の上限（TEXT に収まり、人が読める長さ） */
	static final int MAX_ERROR_MESSAGE = 2000;

	/** error_message に値が入らないよう伏せる設定（API キー・パスワード） */
	static final List<String> SECRET_KEYS = List.of("catticket.kopis.api-key", "catticket.azure.translator-key",
			"catticket.recaptcha.api-key", "catticket.admin.password", "spring.mail.password",
			"spring.datasource.password");

	private final BatchRunMapper mapper;

	private final JsonMapper jsonMapper;

	private final List<String> secrets;

	public BatchRunRecorder(BatchRunMapper mapper, JsonMapper jsonMapper, Environment environment) {
		this.mapper = mapper;
		this.jsonMapper = jsonMapper;
		// 短すぎる値を伏せると普通の文字まで消えるので対象にしない
		this.secrets = SECRET_KEYS.stream()
			.map(environment::getProperty)
			.filter(Objects::nonNull)
			.map(String::strip)
			.filter(v -> v.length() >= 4)
			.toList();
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public long start(TaskName task, LocalDateTime now) {
		JobRunInsert row = new JobRunInsert(task.value(), now);
		mapper.insertJobRun(row);
		return row.getId();
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public StepRun startStep(long jobRunId, String stepName, LocalDateTime now) {
		StepRunInsert row = new StepRunInsert(jobRunId, stepName, now);
		mapper.insertStepRun(row);
		return new StepRun(row.getId(), jobRunId, stepName);
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void finishStep(StepRun step, StepResult result, LocalDateTime now) {
		String summary = result.summary() == null ? null : jsonMapper.writeValueAsString(result.summary());
		mapper.updateStepRun(step.id(), now, result.success() ? "SUCCESS" : "FAILURE", result.processedCount(),
				summary, sanitize(result.errorMessage()));
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void finish(long jobRunId, JobResult result, String errorMessage, LocalDateTime now) {
		if (result == JobResult.LOCKED) {
			// ロックを取れなかった実行は記録を作らない（06 の 5.2）
			throw new IllegalArgumentException("LOCKED is not recorded");
		}
		mapper.updateJobRun(jobRunId, now, result.name(), sanitize(errorMessage));
	}

	/** 例外を error_message の形（クラス名とメッセージ。スタックトレースは入れない）にする */
	public static String describe(Throwable e) {
		String message = e.getMessage();
		return message == null ? e.getClass().getSimpleName() : e.getClass().getSimpleName() + ": " + message;
	}

	/** 1行にし、秘密の値を伏せ、長さを抑える */
	String sanitize(String errorMessage) {
		if (errorMessage == null) {
			return null;
		}
		String value = errorMessage.replaceAll("\\s*[\\r\\n]+\\s*", " ").strip();
		for (String secret : secrets) {
			value = value.replace(secret, "***");
		}
		return Texts.truncateCodePoints(value, MAX_ERROR_MESSAGE);
	}

}
