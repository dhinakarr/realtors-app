package com.realtors.admin.dto;

import java.time.Instant;

public record TokenSession(
        String accessToken,
        String refreshToken,
        Instant issuedAt,
        Instant expiresAt
) {}
