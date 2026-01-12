package com.realtors.projects.repository;


import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.realtors.common.util.AppUtil;
import com.realtors.projects.dto.PlotUnitDto;
import com.realtors.projects.rowmapper.PlotUnitRowMapper;

import java.util.*;

@Repository
public class PlotUnitRepository {

    private final JdbcTemplate jdbc;
    
    public PlotUnitRepository(JdbcTemplate jdbc) {
    	this.jdbc = jdbc;
    }

    // --------------------------
    // INSERT (Single)
    // --------------------------
    public UUID create(PlotUnitDto dto) {

        UUID id = UUID.randomUUID();
        String sql = """
            INSERT INTO plot_units 
            (plot_id, project_id, plot_number, area, base_price, road_width, survey_num, 
             facing, width, length, total_price, is_prime, status, customer_id, remarks, created_at= CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        jdbc.update(sql,
                id,
                dto.getProjectId(),
                dto.getPlotNumber(),
                dto.getArea(),
                dto.getBasePrice(),
                dto.getRoadWidth(),
                dto.getSurveyNum(),
                dto.getFacing(),
                dto.getWidth(),
                dto.getBreath(),
                dto.getTotalPrice(),
                dto.getIsPrime(),
                dto.getStatus(),
                dto.getCustomerId(),
                dto.getRemarks()
        );

        return id;
    }

    // --------------------------
    // BULK INSERT (auto-generate)
    // --------------------------
    public void bulkInsert(List<PlotUnitDto> list) {

        String sql = """
            INSERT INTO plot_units 
            (plot_id, project_id, plot_number, base_price, status, is_prime)
            VALUES (?, ?, ?, ?, ?, ?)
        """;

        List<Object[]> batch = new ArrayList<>();

        for (PlotUnitDto p : list) {
            batch.add(new Object[]{
                    p.getPlotId(),
                    p.getProjectId(),
                    p.getPlotNumber(),
                    p.getBasePrice(),
                    p.getStatus(),
                    p.getIsPrime()
            });
        }

        jdbc.batchUpdate(sql, batch);
    }

    // --------------------------
    // GET BY PROJECT
    // --------------------------
    public List<PlotUnitDto> findByProjectId(UUID projectId) {
        String sql = """
        		SELECT * FROM plot_units WHERE project_id = ?  
        		ORDER BY CAST(NULLIF(regexp_replace(plot_number, '[^0-9]', '', 'g'), '') AS INTEGER) NULLS LAST,
        			regexp_replace(plot_number, '[0-9]', '', 'g');
        		""";

        return jdbc.query(sql, new PlotUnitRowMapper(), projectId);
    }
    
    // --------------------------
    // GET BY PLOT
    // --------------------------
    public PlotUnitDto findByPlotId(UUID plotId) {
        String sql = """
        		SELECT * FROM plot_units WHERE plot_id = ?  
        		ORDER BY  CAST(NULLIF(regexp_replace(plot_number, '[^0-9]', '', 'g'), '') AS INTEGER) NULLS LAST,
        			regexp_replace(plot_number, '[0-9]', '', 'g');
        		""";
        return jdbc.query(sql, new PlotUnitRowMapper(), plotId).getFirst();
    }

    // --------------------------
    // UPDATE
    // --------------------------
    public int update(PlotUnitDto dto) {

        String sql = """
            UPDATE plot_units SET 
              plot_number = ?, area = ?, rate_per_sqft=?, base_price = ?, road_width = ?, survey_num = ?,
              facing = ?, width = ?, breath = ?, total_price = ?, is_prime = ?, 
              status = ?, customer_id = ?, remarks = ?, updated_at = CURRENT_TIMESTAMP
            WHERE plot_id = ?
        """;

        return jdbc.update(sql,
                dto.getPlotNumber(),
                dto.getArea(),
                dto.getRatePerSqft(),
                dto.getBasePrice(),
                dto.getRoadWidth(),
                dto.getSurveyNum(),
                dto.getFacing(),
                dto.getWidth(),
                dto.getBreath(),
                dto.getTotalPrice(),
                dto.getIsPrime(),
                dto.getStatus(),
                dto.getCustomerId(),
                dto.getRemarks(),
                dto.getPlotId()
        );
    }
    
    public void updatePlotStatus(UUID plotId, String status) {
    	String sql = "UPDATE plot_units set status=?, updated_at=CURRENT_TIMESTAMP, updated_by=? where plot_id=?";
    	jdbc.update(sql, status, AppUtil.getCurrentUserId(), plotId);
    }
    
    // --------------------------
    // DELETE
    // --------------------------
    public int delete(UUID id) {
        String updateSql = "UPDATE plot_units SET status='CANCELLED' WHERE plot_id = ?";
        return jdbc.update(updateSql, id);
    }
    
    public boolean deleteByProjectId(UUID projectId) {
    	String updateSql = "DELETE FROM plot_units WHERE project_id = ?";
    	int retValue = jdbc.update(updateSql, projectId);
        return retValue >0;
    }
}

