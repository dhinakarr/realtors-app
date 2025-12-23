package com.realtors.sitevisit.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

import org.springframework.jdbc.core.RowMapper;

import com.realtors.sitevisit.dto.SitePaymentDTO;

public class SitePaymentRowMapper implements RowMapper<SitePaymentDTO> {

    @Override
    public SitePaymentDTO mapRow(ResultSet rs, int rowNum) throws SQLException {

    	SitePaymentDTO dto = new SitePaymentDTO();
        dto.setUserId(UUID.fromString(rs.getString("user_id")));
        dto.setAmount(rs.getBigDecimal("amount"));
        dto.setPaymentMode(rs.getString("payment_mode"));
        dto.setPaymentDate(rs.getDate("payment_date").toLocalDate());
        dto.setRemarks(rs.getString("remarks"));
        return dto;
    }
}
