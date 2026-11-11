package com.catticket.common.error;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.webmvc.error.ErrorController;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * {@code /error}（サーブレットのフィルターなど MVC の外で起きた例外の受け皿）も 03 の 7.3 の形にする。
 * このクラスがあると Spring Boot の既定の BasicErrorController は作られない。
 */
@RestController
public class ApiErrorController implements ErrorController {

	private static final Logger log = LoggerFactory.getLogger(ApiErrorController.class);

	@RequestMapping("${server.error.path:/error}")
	public ResponseEntity<ErrorResponse> error(HttpServletRequest request) {
		ErrorCode code = toCode(request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE));
		if (code.status().is5xxServerError()) {
			Object ex = request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);
			log.error("Error dispatched to /error: {}", request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI),
					ex instanceof Throwable t ? t : null);
		}
		return ResponseEntity.status(code.status()).body(ErrorResponse.of(code));
	}

	// 業務のコードはここまで届かないので、ステータスから共通のコードを選ぶ。分からないものは 500
	private static ErrorCode toCode(Object status) {
		int value = status instanceof Integer i ? i : 500;
		return switch (value) {
			case 400 -> ErrorCode.MALFORMED_REQUEST;
			case 401 -> ErrorCode.UNAUTHENTICATED;
			case 403 -> ErrorCode.FORBIDDEN;
			case 404 -> ErrorCode.NOT_FOUND;
			case 405 -> ErrorCode.METHOD_NOT_ALLOWED;
			case 415 -> ErrorCode.UNSUPPORTED_MEDIA_TYPE;
			default -> ErrorCode.INTERNAL_ERROR;
		};
	}

}
