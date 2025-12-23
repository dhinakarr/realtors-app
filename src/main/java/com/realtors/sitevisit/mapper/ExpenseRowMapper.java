package com.realtors.sitevisit.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

import org.springframework.jdbc.core.RowMapper;

import com.realtors.sitevisit.dto.ExpenseDTO;

public class ExpenseRowMapper implements RowMapper<ExpenseDTO> {

    @Override
    public ExpenseDTO mapRow(ResultSet rs, int rowNum) throws SQLException {

        ExpenseDTO dto = new ExpenseDTO();
        dto.setExpenseTypeId(UUID.fromString(rs.getString("expense_type_id")));
        dto.setAmount(rs.getBigDecimal("amount"));
        dto.setPaidBy(rs.getString("paid_by"));
        dto.setExpenseDate(rs.getDate("expense_date").toLocalDate());
        dto.setBillReference(rs.getString("bill_reference"));
        dto.setRemarks(rs.getString("remarks"));
        return dto;
    }
}
