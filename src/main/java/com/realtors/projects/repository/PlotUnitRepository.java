package com.realtors.projects.repository;

import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.realtors.common.util.AppUtil;
import com.realtors.projects.dto.PlotUnitDto;
import com.realtors.projects.dto.PlotUnitStatus;
import com.realtors.projects.rowmapper.PlotUnitRowMapper;

import java.util.*;

@Repository
public class PlotUnitRepository {

	private final JdbcTemplate jdbc;
	private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
	private static final RowMapper<PlotUnitStatus> ROW_MAPPER = new BeanPropertyRowMapper<>(PlotUnitStatus.class);

	public PlotUnitRepository(JdbcTemplate jdbc, NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
		this.jdbc = jdbc;
		this.namedParameterJdbcTemplate=namedParameterJdbcTemplate; 
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

		jdbc.update(sql, id, dto.getProjectId(), dto.getPlotNumber(), dto.getArea(), dto.getBasePrice(),
				dto.getRoadWidth(), dto.getSurveyNum(), dto.getFacing(), dto.getWidth(), dto.getBreath(),
				dto.getTotalPrice(), dto.getIsPrime(), dto.getStatus(), dto.getCustomerId(), dto.getRemarks());

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
			batch.add(new Object[] { p.getPlotId(), p.getProjectId(), p.getPlotNumber(), p.getBasePrice(),
					p.getStatus(), p.getIsPrime() });
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
		return jdbc.update(sql, dto.getPlotNumber(), dto.getArea(), dto.getRatePerSqft(), dto.getBasePrice(),
				dto.getRoadWidth(), dto.getSurveyNum(), dto.getFacing(), dto.getWidth(), dto.getBreath(),
				dto.getTotalPrice(), dto.getIsPrime(), dto.getStatus(), dto.getCustomerId(), dto.getRemarks(),
				dto.getPlotId());
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
	
	public void syncPlots(UUID projectId, List<String> incomingPlots) {

	    List<String> existingPlots = findPlotNumbersByProjectId(projectId);

	    // Convert to Set for faster lookup
	    Set<String> existingSet = new HashSet<>(existingPlots);
	    Set<String> incomingSet = new HashSet<>(incomingPlots);

	    // ➕ INSERT: incoming - existing
	    List<String> toInsert = incomingSet.stream()
	            .filter(p -> !existingSet.contains(p))
	            .toList();

	    // ❌ DELETE: existing - incoming
	    List<String> toDelete = existingSet.stream()
	            .filter(p -> !incomingSet.contains(p))
	            .toList();

	    // ---- INSERT NEW ----
	    if (!toInsert.isEmpty()) {
	        List<PlotUnitDto> list = new ArrayList<>();

	        for (String plotNo : toInsert) {
	            PlotUnitDto dto = new PlotUnitDto();
	            dto.setPlotId(UUID.randomUUID());
	            dto.setProjectId(projectId);
	            dto.setPlotNumber(plotNo);
	            dto.setStatus("AVAILABLE");
	            dto.setIsPrime(false);
	            list.add(dto);
	        }

	        bulkInsert(list);
	    }

	    // ---- DELETE OLD (only NOT SOLD) ----
	    if (!toDelete.isEmpty()) {
	        deletePlotsByNumbers(projectId, toDelete);
	    }
	}
	
	public void deletePlotsByNumbers(UUID projectId, List<String> plotNumbers) {
	    String sql = """
	        DELETE FROM plot_units pu
	        WHERE pu.project_id = ?
	        AND pu.plot_number IN (:plotNumbers)
	        AND pu.plot_id NOT IN (
	            SELECT plot_id FROM sales
	        )
	    """;

	    MapSqlParameterSource params = new MapSqlParameterSource();
	    params.addValue("projectId", projectId);
	    params.addValue("plotNumbers", plotNumbers);

	    namedParameterJdbcTemplate.update(sql, params);
	}

	public boolean deleteByProjectId(UUID projectId) {
		String sql = """
				    DELETE FROM plot_units
				    WHERE project_id = ?
				    AND plot_id NOT IN (
				        SELECT plot_id FROM sales
				    )
				""";
		int retValue = jdbc.update(sql, projectId);
		return retValue > 0;
	}

	public List<String> findPlotNumbersByProjectId(UUID projectId) {
		String sql = """
				    SELECT plot_number
				    FROM plot_units
				    WHERE project_id = ?
				""";
		return jdbc.queryForList(sql, String.class, projectId);
	}

	public List<PlotUnitStatus> getPlotStats(UUID projectId) {
		String sql = """
				SELECT p.project_id, COUNT(*) AS total,
				    SUM(CASE WHEN p.status = 'AVAILABLE' THEN 1 ELSE 0 END) AS available,
				    SUM(CASE WHEN p.status = 'BOOKED' THEN 1 ELSE 0 END) AS booked,
				    SUM(CASE WHEN p.status = 'SOLD' THEN 1 ELSE 0 END) AS sold,
				    SUM(CASE WHEN p.status = 'CANCELLED' THEN 1 ELSE 0 END) AS cancelled
				FROM plot_units p
				WHERE p.project_id = ?
				GROUP BY p.project_id;

						""";
		return jdbc.query(sql, ROW_MAPPER, projectId);
	}
}
