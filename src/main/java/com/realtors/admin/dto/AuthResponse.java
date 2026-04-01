package com.realtors.admin.dto;

import java.util.UUID;

public record AuthResponse(UUID userId, String accessToken, String refreshToken) {

}
