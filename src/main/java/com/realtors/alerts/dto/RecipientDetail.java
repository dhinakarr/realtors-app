package com.realtors.alerts.dto;

import java.util.UUID;

public record RecipientDetail(UUID userId, String email, String mobile) {

}
