package com.realtors.sitevisit.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.RowMapper;

import com.realtors.customers.dto.CustomerMiniDto;
import com.realtors.sitevisit.dto.SiteVisitResponseDTO;

public class SiteVisitRowMapper implements RowMapper<SiteVisitResponseDTO> {

    @Override
    public SiteVisitResponseDTO mapRow(ResultSet rs, int rowNum) throws SQLException {

        SiteVisitResponseDTO dto = new SiteVisitResponseDTO();
        dto.setSiteVisitId(rs.getObject("site_visit_id", UUID.class));
        dto.setVisitDate(rs.getDate("visit_date").toLocalDate());

        dto.setUserId(rs.getObject("user_id", UUID.class));
        dto.setUserName(rs.getString("username"));

        dto.setProjectId(rs.getObject("project_id", UUID.class));
        dto.setProjectName(rs.getString("project_name"));

        dto.setVehicleType(rs.getString("vehicle_type"));
        dto.setExpenseAmount(rs.getBigDecimal("expense_amount"));

        dto.setTotalPaid(rs.getBigDecimal("total_paid"));
        dto.setBalance(rs.getBigDecimal("balance"));
        dto.setStatus(rs.getString("status"));
        dto.setRemarks(rs.getString("remarks"));

        // 🔹 Convert aggregated customer names → List<CustomerMiniDto>
        String customerNames = rs.getString("customer_names");
        dto.setCustomers(mapCustomers(customerNames));
        return dto;
    }
    
    private List<CustomerMiniDto> mapCustomers(String customerNames) {
        if (customerNames == null || customerNames.isBlank()) {
            return Collections.emptyList();
        }

        return Arrays.stream(customerNames.split(","))
                .map(String::trim)
                .map(name -> {
                    CustomerMiniDto c = new CustomerMiniDto();
                    c.setCustomerName(name);
                    return c;
                })
                .toList();
    }
}
