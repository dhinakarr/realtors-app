package com.realtors.sitevisit.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

import org.springframework.jdbc.core.RowMapper;

import com.realtors.customers.dto.CustomerMiniDto;

public class CustomerRowMapper implements RowMapper<CustomerMiniDto> {

    @Override
    public CustomerMiniDto mapRow(ResultSet rs, int rowNum) throws SQLException {

    	CustomerMiniDto dto = new CustomerMiniDto();
        dto.setCustomerId(UUID.fromString(rs.getString("customer_id")));
        dto.setCustomerName(rs.getString("customer_name"));
        return dto;
    }
}
