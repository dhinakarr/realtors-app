package com.realtors.common.exception;

import com.realtors.common.ApiResponse;

import lombok.extern.slf4j.Slf4j;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.UUID;

@Slf4j

public class GlobalExceptionManager {

	/* ---------------- 404 ---------------- */

	@ExceptionHandler({ NoSuchElementException.class, EmptyResultDataAccessException.class,
			ResourceNotFoundException.class })
	public ResponseEntity<ApiResponse<ApiError>> handleNotFound(RuntimeException ex) {
		return buildError(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "Requested resource was not found", ex, false);
	}

	/* ---------------- 405 ---------------- */

	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	public ResponseEntity<ApiResponse<ApiError>> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex) {

		return buildError(HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED",
				"HTTP method not supported for this endpoint", ex, false);
	}

	/* ---------------- 400 ---------------- */

	@ExceptionHandler({ IllegalArgumentException.class, MissingServletRequestPartException.class,
			HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class })
	public ResponseEntity<ApiResponse<ApiError>> handleBadRequest(Exception ex) {
		return buildError(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "Invalid request payload or parameters", ex, false);
	}

	/* ---------------- Validation ---------------- */

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiResponse<ApiError>> handleValidation(MethodArgumentNotValidException ex) {

		String message = ex.getBindingResult().getFieldErrors().stream()
				.map(err -> err.getField() + " " + err.getDefaultMessage()).findFirst().orElse("Validation failed");

		return buildError(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message, ex, false);
	}

	/* ---------------- File upload ---------------- */

	@ExceptionHandler(MaxUploadSizeExceededException.class)
	public ResponseEntity<ApiResponse<ApiError>> handleFileSize(MaxUploadSizeExceededException ex) {
		return buildError(HttpStatus.BAD_REQUEST, "FILE_TOO_LARGE", "Uploaded file exceeds the allowed size", ex,
				false);
	}

	/* ---------------- 500 ---------------- */

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiResponse<ApiError>> handleGeneric(Exception ex) {
		return buildError(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR",
				"Something went wrong. Please contact support.", ex, true);
	}

	/* ---------------- Helper ---------------- */

	private ResponseEntity<ApiResponse<ApiError>> buildError(HttpStatus status, String code, String message, Exception ex,
			boolean isCritical) {
		String traceId = UUID.randomUUID().toString();

		if (isCritical) {
			log.error("[{}] {}", traceId, ex.getMessage(), ex);
		} else {
			log.warn("[{}] {}", traceId, ex.getMessage());
		}

		ApiError error = ApiError.builder().code(code).message(message).traceId(traceId).timestamp(Instant.now())
				.build();

		return ResponseEntity.status(status).body(ApiResponse.failure(error.toString()));
	}
}
