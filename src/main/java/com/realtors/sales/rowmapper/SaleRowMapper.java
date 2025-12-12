package com.realtors.sales.rowmapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.UUID;

import org.springframework.jdbc.core.RowMapper;

import com.realtors.sales.dto.SaleDTO;

public class SaleRowMapper implements RowMapper<SaleDTO> {

    @Override
    public SaleDTO mapRow(ResultSet rs, int rowNum) throws SQLException {
        SaleDTO dto = new SaleDTO();
        dto.setSaleId(UUID.fromString(rs.getString("sale_id")));
        dto.setPlotId(UUID.fromString(rs.getString("plot_id")));
        dto.setProjectId(UUID.fromString(rs.getString("project_id")));
        dto.setCustomerId(UUID.fromString(rs.getString("customer_id")));
        dto.setSoldBy(UUID.fromString(rs.getString("sold_by")));
        dto.setArea(rs.getBigDecimal("area"));
        dto.setBasePrice(rs.getBigDecimal("base_price"));
        dto.setExtraCharges(rs.getBigDecimal("extra_charges"));
        dto.setTotalPrice(rs.getBigDecimal("total_price"));
        dto.setSaleStatus(rs.getString("sale_status"));

        Timestamp ts = rs.getTimestamp("confirmed_at"); 
        dto.setConfirmedAt(ts != null ? ts.toLocalDateTime() : null);

        return dto;
    }
}

