package com.realtors.sales.repository;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.realtors.sales.dto.SaleDTO;
import com.realtors.sales.dto.SalesStatus;
import com.realtors.sales.rowmapper.SaleRowMapper;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class SaleRepositoryImpl implements SaleRepository {

    private final JdbcTemplate jdbc;

    @Override
    public SaleDTO createSale(UUID plotId, UUID projectId, UUID customerId, UUID soldBy, BigDecimal area,
                              BigDecimal basePrice, BigDecimal extraCharges, BigDecimal totalPrice) {

        String sql = """
            INSERT INTO sales 
            (plot_id, project_id, customer_id, sold_by, area, base_price, extra_charges, total_price, sale_status)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            RETURNING sale_id, plot_id, project_id, customer_id, sold_by, 
                      area, base_price, extra_charges, total_price, sale_status, confirmed_at
        """;
        
        return jdbc.queryForObject(sql, new SaleRowMapper(),
                plotId, projectId, customerId, soldBy, area,
                basePrice, extraCharges, totalPrice, SalesStatus.BOOKED.toString());
    }

    @Override
    public SaleDTO findById(UUID saleId) {
        String sql = """
            SELECT sale_id, plot_id, project_id, customer_id, sold_by, area,
                   base_price, extra_charges, total_price, sale_status, confirmed_at
            FROM sales WHERE sale_id = ?
        """;

        return jdbc.queryForObject(sql, new SaleRowMapper(), saleId);
    }
    
    @Override
    public void updateSaleStatus(UUID saleId, String status, LocalDateTime confirmedAt) {
        String sql = """
            UPDATE sales
            SET sale_status = ?, confirmed_at = ?, updated_at = now()
            WHERE sale_id = ?
        """;
        jdbc.update(sql, status, Timestamp.valueOf(confirmedAt), saleId);
    }
}
