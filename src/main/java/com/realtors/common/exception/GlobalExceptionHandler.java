package com.realtors.common.exception;


import com.realtors.common.ApiResponse;

import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
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
                .body(ApiResponse.failure(
                        "Internal server error",
                        HttpStatus.INTERNAL_SERVER_ERROR
                ));
    }
}

