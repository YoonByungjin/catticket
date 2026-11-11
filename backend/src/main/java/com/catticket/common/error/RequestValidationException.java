package com.catticket.common.error;

import java.util.List;

/**
 * Bean Validation で書けない入力チェックの誤り。コードはいつも {@link ErrorCode#VALIDATION_FAILED}。
 */
public class RequestValidationException extends ApiException {

	private static final long serialVersionUID = 1L;

	private final List<FieldErrorItem> errors;

	public RequestValidationException(List<FieldErrorItem> errors) {
		super(ErrorCode.VALIDATION_FAILED);
		this.errors = List.copyOf(errors);
	}

	public List<FieldErrorItem> errors() {
		return errors;
	}

}
