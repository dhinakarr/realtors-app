package com.realtors.sitevisit.service;

import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.realtors.sitevisit.dto.VehicleUsageDTO;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SiteVisitVehicleServiceImpl implements SiteVisitVehicleService {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void saveVehicle(UUID siteVisitId, VehicleUsageDTO v) {

        if (v == null) return;

        jdbcTemplate.update("""
            INSERT INTO site_visit_vehicles
            (site_visit_id, vehicle_id, fuel_cost, driver_cost, toll_cost, rent_cost)
            VALUES (?, ?, ?, ?, ?, ?)
        """,
            siteVisitId,
            v.getVehicleId(),
            v.getFuelCost(),
            v.getDriverCost(),
            v.getTollCost(),
            v.getRentCost()
        );
    }
}
