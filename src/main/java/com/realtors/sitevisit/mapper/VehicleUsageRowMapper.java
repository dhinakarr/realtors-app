package com.realtors.sitevisit.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

import org.springframework.jdbc.core.RowMapper;

import com.realtors.sitevisit.dto.VehicleUsageDTO;

public class VehicleUsageRowMapper implements RowMapper<VehicleUsageDTO> {

    @Override
    public VehicleUsageDTO mapRow(ResultSet rs, int rowNum) throws SQLException {

        VehicleUsageDTO dto = new VehicleUsageDTO();
        dto.setVehicleId(UUID.fromString(rs.getString("vehicle_id")));
        dto.setFuelCost(rs.getBigDecimal("fuel_cost"));
        dto.setDriverCost(rs.getBigDecimal("driver_cost"));
        dto.setTollCost(rs.getBigDecimal("toll_cost"));
        dto.setRentCost(rs.getBigDecimal("rent_cost"));
        return dto;
    }
}
