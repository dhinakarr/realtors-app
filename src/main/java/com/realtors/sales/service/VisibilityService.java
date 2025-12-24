package com.realtors.sales.service;

import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.realtors.sales.dto.SaleDTO;
import com.realtors.sales.rowmapper.SaleRowMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class VisibilityService {

    private final JdbcTemplate jdbc;

    public List<SaleDTO> getVisibleSales(UUID currentUserId) {

        String sql = """
            WITH RECURSIVE manager_tree AS (
                SELECT user_id
                FROM app_users
                WHERE user_id = ?

                UNION ALL

                SELECT au.user_id
                FROM app_users au
                INNER JOIN manager_tree mt ON au.manager_id = mt.user_id
            )
            SELECT s.sale_id, s.plot_id, s.project_id, s.customer_id, s.sold_by,
                   s.base_price, s.extra_charges, s.total_price, s.sale_status, s.confirmed_at, p.area
            FROM sales s
            JOIN plot_units p ON p.plot_id = s.plot_id
            WHERE s.sold_by IN (SELECT user_id FROM manager_tree)
            ORDER BY s.confirmed_at DESC
        """;

        return jdbc.query(sql, new SaleRowMapper(), currentUserId);
    }
}
