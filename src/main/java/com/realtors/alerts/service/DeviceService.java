package com.realtors.alerts.service;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.realtors.alerts.dto.DeviceRegisterRequest;
import com.realtors.alerts.repository.UserDeviceRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DeviceService {

    private final UserDeviceRepository repo;

    public void register(UUID userId, DeviceRegisterRequest req) {

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
}
