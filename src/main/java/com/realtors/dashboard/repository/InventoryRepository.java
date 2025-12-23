package com.realtors.dashboard.repository;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.realtors.dashboard.dto.InventoryStatsDTO;
import com.realtors.dashboard.dto.ProjectInventoryStatsDTO;
import com.realtors.dashboard.dto.ProjectInventoryStatsRowMapper;

@Repository
public class InventoryRepository {

    private final JdbcTemplate jdbcTemplate;

    public InventoryRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Overall inventory (Donut chart)
     */
    public List<InventoryStatsDTO> getOverallInventory() {

        String sql = """
            SELECT
                inventory_status AS status,
                COUNT(*) AS count
            FROM v_inventory_status
            GROUP BY inventory_status
        """;

        return jdbcTemplate.query(
            sql,
            new InventoryStatsRowMapper()
        );
    }

    /**
     * Project-wise inventory (Bar chart)
     */
    public List<ProjectInventoryStatsDTO> getProjectWiseInventory() {

        String sql = """
            SELECT
                project_id,
                project_name,
                inventory_status,
                COUNT(*) AS count
            FROM v_inventory_status
            GROUP BY project_id, project_name, inventory_status
        """;

        return jdbcTemplate.query(
            sql,
            new ProjectInventoryStatsRowMapper()
        );
    }
}
