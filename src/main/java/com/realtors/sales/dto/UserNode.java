package com.realtors.sales.dto;

import java.util.UUID;

public record UserNode(
        UUID userId,
        UUID roleId,
        int roleLevel
) {}

