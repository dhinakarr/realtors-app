package com.realtors.sales.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.realtors.sales.dto.PaymentStageDTO;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class PaymentStageRepositoryImpl implements PaymentStageRepository {

	private final JdbcTemplate jdbc;

	@Override
	public PaymentStageDTO insertStage(PaymentStageDTO dto) {

	    String sql = """
	        INSERT INTO payment_stages
	        (project_id, stage_name, description, percentage, sequence)
	        VALUES (?, ?, ?, ?, ?)
	        RETURNING stage_id, project_id, stage_name, description, percentage, sequence, created_at
	    """;

	    return jdbc.queryForObject(sql,
	            (rs, row) -> new PaymentStageDTO(
	                    UUID.fromString(rs.getString("stage_id")),
	                    UUID.fromString(rs.getString("project_id")),
	                    rs.getString("stage_name"),
	                    rs.getString("description"),
	                    rs.getBigDecimal("percentage"),
	                    rs.getObject("sequence", Integer.class),
	                    rs.getTimestamp("created_at")
	            ),
	            dto.getProjectId(),
	            dto.getStageName(),
	            dto.getDescription(),
	            dto.getPercentage(),
	            dto.getSequence()
	    );
	}

	@Override
	public List<PaymentStageDTO> listStages(UUID projectId) {

		String sql = """
				    SELECT stage_id, project_id, stage_name, percentage
				    FROM payment_stages
				    WHERE project_id = ?
				    ORDER BY stage_name
				""";

		return jdbc.query(sql, (rs, row) -> {
		    PaymentStageDTO dto = new PaymentStageDTO();
		    dto.setStageId(UUID.fromString(rs.getString("stage_id")));
		    dto.setProjectId(UUID.fromString(rs.getString("project_id")));
		    dto.setStageName(rs.getString("stage_name"));
		    dto.setDescription(rs.getString("description"));
		    dto.setPercentage(rs.getBigDecimal("percentage"));
		    dto.setSequence(rs.getInt("sequence"));
		    dto.setCreatedAt(rs.getTimestamp("created_at"));
		    return dto;
		}, projectId);
	}

	@Override
	public void updateStage(UUID stageId, PaymentStageDTO dto) {
		String sql = """
				    UPDATE payment_stages
				    SET stage_name = ?, percentage = ?
				    WHERE stage_id = ?
				""";
		jdbc.update(sql, dto.getStageName(), dto.getPercentage(), stageId);
	}

	@Override
	public void deleteStage(UUID stageId) {
		jdbc.update("DELETE FROM payment_stages WHERE stage_id = ?", stageId);
	}
}
