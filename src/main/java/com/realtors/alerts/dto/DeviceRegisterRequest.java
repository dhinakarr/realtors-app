package com.realtors.alerts.dto;

import lombok.Data;

@Data
public class DeviceRegisterRequest {
    private String deviceToken;
    private DevicePlatform platform;
}
