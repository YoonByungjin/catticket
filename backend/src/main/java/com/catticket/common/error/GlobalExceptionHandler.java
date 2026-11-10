package com.catticket.common.error;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.MethodParameter;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.validation.method.ParameterErrors;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 例外を 03 の 7.3 のエラーの本文にする（06 の 3.2 の表）。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	/** 型の変換の失敗の理由（03 の 7.3） */
	static final String REASON_TYPE = "TYPE";

	static final String REASON_REQUIRED = "REQUIRED";

	/** クエリの繰り返しの値の添字（genre[1]）。画面はクエリの名前だけで欄を決めるので除く */
	private static final Pattern INDEX = Pattern.compile("\\[\\d+]");

	@ExceptionHandler(RequestValidationException.class)
	ResponseEntity<ErrorResponse> handleRequestValidation(RequestValidationException ex) {
		return validationFailed(ex.errors());
	}

	@ExceptionHandler(ApiException.class)
	ResponseEntity<ErrorResponse> handleApi(ApiException ex) {
		return respond(ex.code(), List.of(), ex);
	}

	// @Valid の本文・クエリの record の両方がここに来る（MethodArgumentNotValidException は BindException の子）
	@ExceptionHandler(BindException.class)
	ResponseEntity<ErrorResponse> handleBind(BindException ex) {
		boolean body = ex instanceof MethodArgumentNotValidException manv && isRequestBody(manv.getParameter());
		return validationFailed(toItems(ex.getBindingResult(), body));
	}

	@ExceptionHandler(HandlerMethodValidationException.class)
	ResponseEntity<ErrorResponse> handleMethodValidation(HandlerMethodValidationException ex) {
		List<FieldErrorItem> items = new ArrayList<>();
		for (ParameterValidationResult result : ex.getParameterValidationResults()) {
			if (result instanceof ParameterErrors errors) {
				items.addAll(toItems(errors, isRequestBody(result.getMethodParameter())));
				continue;
			}
			String name = result.getMethodParameter().getParameterName();
			result.getResolvableErrors().forEach(e -> items.add(new FieldErrorItem(name, e.getDefaultMessage())));
		}
		return validationFailed(items);
	}

	@ExceptionHandler(MissingServletRequestParameterException.class)
	ResponseEntity<ErrorResponse> handleMissingParameter(MissingServletRequestParameterException ex) {
		return validationFailed(List.of(new FieldErrorItem(ex.getParameterName(), REASON_REQUIRED)));
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
		return validationFailed(List.of(new FieldErrorItem(ex.getName(), REASON_TYPE)));
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	ResponseEntity<ErrorResponse> handleNotReadable(HttpMessageNotReadableException ex) {
		return respond(ErrorCode.MALFORMED_REQUEST, List.of(), ex);
	}

	@ExceptionHandler(HttpMediaTypeNotSupportedException.class)
	ResponseEntity<ErrorResponse> handleMediaType(HttpMediaTypeNotSupportedException ex) {
		return respond(ErrorCode.UNSUPPORTED_MEDIA_TYPE, List.of(), ex);
	}

	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	ResponseEntity<ErrorResponse> handleMethod(HttpRequestMethodNotSupportedException ex) {
		return respond(ErrorCode.METHOD_NOT_ALLOWED, List.of(), ex);
	}

	@ExceptionHandler({ NoResourceFoundException.class, NoHandlerFoundException.class })
	ResponseEntity<ErrorResponse> handleNotFound(Exception ex) {
		return respond(ErrorCode.NOT_FOUND, List.of(), ex);
	}

	@ExceptionHandler(Exception.class)
	ResponseEntity<ErrorResponse> handleOther(Exception ex) {
		return respond(ErrorCode.INTERNAL_ERROR, List.of(), ex);
	}

	private ResponseEntity<ErrorResponse> validationFailed(List<FieldErrorItem> items) {
		// 同じ項目・同じ理由の重複（2つの制約が同じ理由で当たったとき）は1つにする
		List<FieldErrorItem> distinct = List.copyOf(new LinkedHashSet<>(items));
		log.info("Request validation failed: {} field error(s)", distinct.size());
		return ResponseEntity.status(ErrorCode.VALIDATION_FAILED.status())
			.body(ErrorResponse.of(ErrorCode.VALIDATION_FAILED, distinct));
	}

	private ResponseEntity<ErrorResponse> respond(ErrorCode code, List<FieldErrorItem> items, Exception ex) {
		if (code.status().is5xxServerError()) {
			log.error("Unhandled error", ex);
		}
		else {
			// 4xx は入力の内容を出さない（パスワードなどが入りうる）
			log.info("Request rejected: {}", code);
		}
		return ResponseEntity.status(code.status()).body(ErrorResponse.of(code, items));
	}

	private static List<FieldErrorItem> toItems(Errors result, boolean body) {
		List<FieldErrorItem> items = new ArrayList<>();
		for (ObjectError error : result.getAllErrors()) {
			if (error instanceof FieldError fe) {
				String field = body ? fe.getField() : INDEX.matcher(fe.getField()).replaceAll("");
				String reason = fe.isBindingFailure() ? REASON_TYPE : fe.getDefaultMessage();
				items.add(new FieldErrorItem(field, reason));
			}
			else {
				// クラスに付けた制約で項目を指定しなかったもの
				items.add(new FieldErrorItem(error.getObjectName(), error.getDefaultMessage()));
			}
		}
		return items;
	}

	private static boolean isRequestBody(MethodParameter parameter) {
		return parameter != null && parameter.hasParameterAnnotation(RequestBody.class);
	}

}
