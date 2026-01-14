package com.realtors.sales.rowmapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import com.realtors.sales.dto.PaymentRuleDto;

@Component
public class PaymentRuleRowMapper implements RowMapper<PaymentRuleDto> {

    @Override
    public PaymentRuleDto mapRow(ResultSet rs, int rowNum) throws SQLException {
    	PaymentRuleDto r = new PaymentRuleDto();
        r.setRuleId(rs.getObject("rule_id", UUID.class));
        r.setProjectId(rs.getObject("project_id", UUID.class));
        r.setRoleId(rs.getObject("role_id", UUID.class));
        r.setUserId(rs.getObject("user_id", UUID.class));
        r.setCommissionType(rs.getString("commission_type"));
        r.setCommissionValue(rs.getBigDecimal("commission_value"));
        r.setPriority(rs.getInt("priority"));
        r.setActive(rs.getBoolean("active"));
        r.setEffectiveFrom(rs.getObject("effective_from", LocalDate.class));
        r.setEffectiveTo(rs.getObject("effective_to", LocalDate.class));
        r.setCreatedAt(rs.getObject("created_at", OffsetDateTime.class));
        r.setUpdatedAt(rs.getObject("updated_at", OffsetDateTime.class));
        r.setCreatedBy(rs.getObject("created_by", UUID.class));
        r.setUpdatedBy(rs.getObject("updated_by", UUID.class));
        return r;
    }
}
