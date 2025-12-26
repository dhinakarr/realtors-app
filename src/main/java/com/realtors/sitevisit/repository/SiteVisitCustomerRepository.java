package com.realtors.sitevisit.repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.realtors.customers.dto.CustomerMiniDto;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class SiteVisitCustomerRepository {

    private final JdbcTemplate jdbcTemplate;

    public void insert(UUID siteVisitId, List<UUID> customerIds) {
        String sql = """
            INSERT INTO site_visit_customers (site_visit_id, customer_id)
            VALUES (?, ?)
        """;

        jdbcTemplate.batchUpdate(sql,
            customerIds,
            customerIds.size(),
            (ps, customerId) -> {
                ps.setObject(1, siteVisitId);
                ps.setObject(2, customerId);
            }
        );
    }
    
    public List<CustomerMiniDto> findCustomers(UUID siteVisitId) {

        return jdbcTemplate.query("""
            SELECT
                c.customer_id,
                c.customer_name,
                c.mobile,
                c.email
            FROM site_visit_customers svc
            JOIN customers c
              ON c.customer_id = svc.customer_id
            WHERE svc.site_visit_id = ?
            ORDER BY c.customer_name
        """,
        (rs, rowNum) -> new CustomerMiniDto(
            rs.getObject("customer_id", UUID.class),
            rs.getString("customer_name"),
            rs.getLong("mobile"), 
            rs.getString("email"),null
        ),
        siteVisitId
        );
    }
    
    public Map<UUID, List<CustomerMiniDto>> findBySiteVisitIds(
            List<UUID> siteVisitIds) {

        if (siteVisitIds == null || siteVisitIds.isEmpty()) {
            return Map.of();
        }

        String placeholders = siteVisitIds.stream()
                .map(id -> "?")
                .collect(Collectors.joining(","));

        String sql = """
            SELECT
                svc.site_visit_id,
                c.customer_id,
                c.customer_name,
                c.mobile,
                c.email
            FROM site_visit_customers svc
            JOIN customers c
              ON c.customer_id = svc.customer_id
            WHERE svc.site_visit_id IN (""" + placeholders + ")";

        return jdbcTemplate.query(
            sql,
            siteVisitIds.toArray(),
            rs -> {
                Map<UUID, List<CustomerMiniDto>> map = new HashMap<>();

                while (rs.next()) {
                    UUID visitId =
                            rs.getObject("site_visit_id", UUID.class);

                    map.computeIfAbsent(visitId, k -> new ArrayList<>())
                       .add(new CustomerMiniDto(
                           rs.getObject("customer_id", UUID.class),
                           rs.getString("customer_name"),
                           rs.getLong("mobile"), 
                           rs.getString("email"),
                           null
                       ));
                }
                return map;
            }
        );
    }
    
    public void replace(UUID siteVisitId, List<UUID> customerIds) {

        // 1️⃣ Delete existing customers for this visit
        jdbcTemplate.update("""
            DELETE FROM site_visit_customers
            WHERE site_visit_id = ?
        """, siteVisitId);

        // 2️⃣ Insert new customers
        if (customerIds != null && !customerIds.isEmpty()) {
            String sql = """
                INSERT INTO site_visit_customers
                (site_visit_id, customer_id)
                VALUES (?, ?)
            """;

            jdbcTemplate.batchUpdate(
                sql,
                customerIds,
                customerIds.size(),
                (ps, customerId) -> {
                    ps.setObject(1, siteVisitId);
                    ps.setObject(2, customerId);
                }
            );
        }
    }

    
}
