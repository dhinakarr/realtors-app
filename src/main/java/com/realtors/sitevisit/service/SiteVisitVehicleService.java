package com.realtors.sitevisit.service;

import java.util.UUID;
import com.realtors.sitevisit.dto.VehicleUsageDTO;

public interface SiteVisitVehicleService {
    void saveVehicle(UUID siteVisitId, VehicleUsageDTO vehicle);
}