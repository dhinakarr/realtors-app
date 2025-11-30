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
    
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse> handleNoResource(NoResourceFoundException ex, HttpServletRequest request) {

        // Ignore Chrome devtools probing request
        if (request.getRequestURI().startsWith("/.well-known/")) {
            return ResponseEntity.notFound().build();
        }

        // otherwise your existing logic
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.failure("Resource not found", HttpStatus.NOT_FOUND));
    }

    @ExceptionHandler({NoSuchElementException.class, 
    					EmptyResultDataAccessException.class, 
    					ResourceNotFoundException.class})
    public ResponseEntity<ApiResponse> handleNotFound(Exception ex) {
        log.error("Resource not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.failure("Resource not found", HttpStatus.NOT_FOUND));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse> handleGeneric(Exception ex) {
        log.error("Unexpected error: ", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.failure("Internal server error", HttpStatus.INTERNAL_SERVER_ERROR));
    }
}

