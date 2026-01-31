package com.realtors.common.exception;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class ApiError {
    private String code;
    private String message;
    private String traceId;
    private Instant timestamp;
}
