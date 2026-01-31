package com.realtors.alerts.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.realtors.alerts.dto.DeviceRegisterRequest;
import com.realtors.alerts.repository.UserDeviceRepository;
import com.realtors.common.EnumConstants;
import com.realtors.common.service.AuditTrailService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DeviceService {

    private final UserDeviceRepository repo;
    private final AuditTrailService audit; 
    private String TABLE_NAME = "user_devices";

    public void register(UUID userId, DeviceRegisterRequest req) {
    	audit.auditAsync(TABLE_NAME, userId, EnumConstants.CREATE);
        repo.findDeviceIdByToken(req.getDeviceToken())
            .ifPresentOrElse(
                repo::touchDevice,
                () -> repo.saveDevice(
                        userId,
                        req.getDeviceToken(),
                        req.getPlatform()
                )
            );
    }
    
    public List<String> getActiveTokenByUserId(UUID userId) {
    	return repo.findActiveTokens(userId);
    }
}
