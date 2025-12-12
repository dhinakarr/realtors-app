package com.realtors.sales.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.realtors.sales.dto.SaleCommissionDTO;
import com.realtors.sales.dto.SaleDTO;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ReportsRepositoryImpl implements ReportsRepository {

    private final JdbcTemplate jdbc;

    @Override
    public List<SaleDTO> getSalesReport(LocalDate from, LocalDate to) {

        String sql = """
            SELECT s.sale_id, s.project_id, s.plot_id, s.customer_id, s.area,
                   s.total_amount, s.created_at
            FROM sales s
            WHERE DATE(s.created_at) BETWEEN ? AND ?
            ORDER BY s.created_at DESC
        """;

        return jdbc.query(sql, (rs, row) ->
                new SaleDTO(
                        UUID.fromString(rs.getString("sale_id")),
                        UUID.fromString(rs.getString("project_id")),
                        UUID.fromString(rs.getString("plot_id")),
                        UUID.fromString(rs.getString("customer_id")),
                        rs.getBigDecimal("area"),
                        rs.getBigDecimal("total_amount"),
                        rs.getTimestamp("created_at").toLocalDateTime()
                ),
                from, to
        );
    }

    @Override
    public List<SaleCommissionDTO> getCommissionReport(UUID userId) {

        String sql = """
            SELECT commission_id, sale_id, user_id, role_id,
                   percentage, commission_amount, created_at
            FROM sale_commissions
            WHERE user_id = ?
            ORDER BY created_at DESC
        """;

        return jdbc.query(sql, (rs, row) ->
                new SaleCommissionDTO(
                        UUID.fromString(rs.getString("commission_id")),
                        UUID.fromString(rs.getString("sale_id")),
                        UUID.fromString(rs.getString("user_id")),
                        UUID.fromString(rs.getString("role_id")),
                        rs.getBigDecimal("percentage"),
                        rs.getBigDecimal("commission_amount"),
                        rs.getTimestamp("created_at")
                ), userId
        );
    }
}

