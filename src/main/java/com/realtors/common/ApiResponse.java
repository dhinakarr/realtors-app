package com.realtors.common;

import org.springframework.http.HttpStatus;

public class ApiResponse<T> {

    private boolean success;
    private String message;
    private T data;
    private HttpStatus status;

    private ApiResponse(boolean success, String message, T data, HttpStatus status) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.status = status;
    }

    // Success factory methods
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, data, HttpStatus.OK);
    }
    

    public static <T> ApiResponse<T> success(String message, T data, HttpStatus status) {
        return new ApiResponse<>(true, message, data, status);
    }

    // Failure factory methods
    public static <T> ApiResponse<T> failure(String message) {
        return new ApiResponse<>(false, message, null, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    public static <T> ApiResponse<T> failure(String message, HttpStatus status) {
        return new ApiResponse<>(false, message, null, status);
    }

    // Getters
    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public T getData() { return data; }
    public HttpStatus getStatus() { return status; }
}
