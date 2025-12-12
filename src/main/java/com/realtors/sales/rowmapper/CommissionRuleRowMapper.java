package com.realtors.sales.rowmapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

import org.springframework.jdbc.core.RowMapper;

import com.realtors.sales.dto.CommissionRuleDTO;

public class CommissionRuleRowMapper implements RowMapper<CommissionRuleDTO> {

    @Override
    public CommissionRuleDTO mapRow(ResultSet rs, int rowNum) throws SQLException {
        CommissionRuleDTO dto = new CommissionRuleDTO();
        dto.setRuleId(UUID.fromString(rs.getString("rule_id")));
        dto.setProjectId(UUID.fromString(rs.getString("project_id")));
        dto.setRoleId(UUID.fromString(rs.getString("role_id")));	
        dto.setPercentage(rs.getBigDecimal("percentage"));
        dto.setRoleName(rs.getString("role_name"));
        return dto;
    }
}
