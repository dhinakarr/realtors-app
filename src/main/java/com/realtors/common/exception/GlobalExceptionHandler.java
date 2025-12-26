package com.realtors.common.exception;


import com.realtors.common.ApiResponse;

import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.NoSuchElementException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    /* ---------------- 404 ---------------- */

    @ExceptionHandler({
        NoSuchElementException.class,
        EmptyResultDataAccessException.class,
        ResourceNotFoundException.class
    })
    public ResponseEntity<ApiResponse> handleNotFound(RuntimeException ex) {
        log.error("Resource not found", ex);
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.failure(ex.getMessage(), HttpStatus.NOT_FOUND));
    }

    /* ---------------- 400 ---------------- */

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse> handleBadRequest(IllegalArgumentException ex) {
        log.warn("Bad request", ex);
        return ResponseEntity.badRequest()
                .body(ApiResponse.failure(ex.getMessage(), HttpStatus.BAD_REQUEST));
    }

    /* ---------------- 500 ---------------- */

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse> handleGeneric(Exception ex) {
        log.error("Unhandled exception", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.failure(ex.getMessage()));
    }
    
    @ExceptionHandler({
        MissingServletRequestPartException.class,
        HttpMessageNotReadableException.class,
        MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<ApiResponse<Void>> handleRequestParsing(Exception ex) {
        log.warn("Request parsing error", ex);
        return ResponseEntity.badRequest()
                .body(ApiResponse.failure(ex.getMessage(), HttpStatus.BAD_REQUEST));
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .findFirst()
                .orElse("Validation failed");

        return ResponseEntity.badRequest()
                .body(ApiResponse.failure(message, HttpStatus.BAD_REQUEST));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleFileSize(MaxUploadSizeExceededException ex) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.failure("Uploaded file size exceeds the allowed limit",
                        HttpStatus.BAD_REQUEST));
    }


}

