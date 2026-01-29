package com.realtors.projects.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Repository;

import com.realtors.common.util.AppUtil;
import com.realtors.projects.dto.ProjectDocumentDto;
import com.realtors.projects.dto.ProjectDto;
import com.realtors.projects.dto.ProjectFileDto;
import com.realtors.projects.dto.ProjectSummaryDto;
import com.realtors.projects.rowmapper.ProjectRowMapper;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ProjectRepository {

	private final JdbcTemplate jdbc;
	private final ProjectRowMapper rowMapper = new ProjectRowMapper();
	private static final Logger logger = LoggerFactory.getLogger(ProjectRepository.class);

	public List<ProjectDto> findAll() {
		String sql = "SELECT * FROM projects ORDER BY created_at DESC";
		return jdbc.query(sql, new ProjectRowMapper());
	}

	public ProjectDto findById(UUID id) {
		String sql = "SELECT * FROM projects WHERE project_id = ?";
		return jdbc.queryForObject(sql, new ProjectRowMapper(), id);
	}

	public ProjectDto create(ProjectDto dto) {
		String sql = """
				INSERT INTO projects
				(project_name, location_details, survey_number,
				 start_date, end_date, plot_numbers, no_of_plots, price_per_sqft, reg_charges,
				 doc_charges, other_charges, guidance_value, status,
				 created_by, updated_by, created_at, updated_at)
				VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING *;
				""";
		UUID userId = AppUtil.getCurrentUserId();
		// 3. Prepare the value array, ensuring all fields are included in order
		Object[] args = new Object[] { dto.getProjectName(), dto.getLocationDetails(), dto.getSurveyNumber(),
				dto.getStartDate(), dto.getEndDate(), dto.getPlotNumbers(), dto.getNoOfPlots(), dto.getPricePerSqft(),
				dto.getRegCharges(), dto.getDocCharges(), dto.getOtherCharges(), dto.getGuidanceValue(),
				dto.getStatus(), // The missing 'status' field
				userId, userId, OffsetDateTime.now(), OffsetDateTime.now() };

		// 4. Use queryForObject with the RowMapper to return the inserted DTO
		return jdbc.queryForObject(sql, args, rowMapper);
	}

	public int update(ProjectDto dto) {
		String sql = """
				    UPDATE projects SET
				      project_name=?, location_details=?, survey_number=?, start_date=?,
				      end_date=?, plot_numbers=?, no_of_plots=?, price_per_sqft=?, reg_charges=?,
				      doc_charges=?, other_charges=?, guidance_value=?, images_location=?,
				      meta=?, updated_at=NOW()
				    WHERE project_id=?;
				""";

		return jdbc.update(sql, dto.getProjectName(), dto.getLocationDetails(), dto.getSurveyNumber(),
				dto.getStartDate(), dto.getEndDate(), dto.getPlotNumbers(), dto.getNoOfPlots(), dto.getPricePerSqft(),
				dto.getRegCharges(), dto.getDocCharges(), dto.getOtherCharges(), dto.getGuidanceValue());
	}

	public int delete(UUID id) {
		return jdbc.update("DELETE FROM projects WHERE project_id = ?", id);
	}

	// this helps to fetch the project and its dependent files
	public List<ProjectSummaryDto> getProjects(UUID filter) {

		String sql;
		Object[] params;

		if (filter == null) {
			sql = """
					SELECT
					    p.project_id, p.project_name, p.location_details, p.survey_number,
					    p.start_date, p.end_date, p.plot_numbers,
					    p.no_of_plots, p.plot_start_number,
					    p.price_per_sqft, p.reg_charges, p.doc_charges,
					    p.other_charges, p.guidance_value,
					    p.created_at AS p_created_at,
					    p.updated_at AS p_updated_at,
					    p.status,

					    -- project_files (existing)
					    f.project_file_id,
					    f.project_id AS f_project_id,
					    f.file_path AS f_file_path,
					    f.file_name AS f_file_name,
					    f.public_url AS f_public_url,
					    f.size_bytes AS f_size_bytes,

					    -- project_documents (new)
					    d.document_id,
					    d.project_id AS d_project_id,
					    d.document_type,
					    d.document_number,
					    d.file_name AS d_file_name,
					    d.file_path AS d_file_path,
					    d.uploaded_by,
					    d.uploaded_at

					FROM projects p

					LEFT JOIN (
					    SELECT *, ROW_NUMBER() OVER (
					        PARTITION BY project_id ORDER BY created_at ASC
					    ) rn
					    FROM projects_files
					) f ON p.project_id = f.project_id AND f.rn = 1

					LEFT JOIN project_documents d
					       ON p.project_id = d.project_id

					WHERE p.status = 'ACTIVE'
					ORDER BY p.created_at DESC;
					""";

			params = new Object[] {};

		} else {
			sql = """
					SELECT
					    p.project_id, p.project_name, p.location_details, p.survey_number,
					    p.start_date, p.end_date, p.plot_numbers,
					    p.no_of_plots, p.plot_start_number,
					    p.price_per_sqft, p.reg_charges, p.doc_charges,
					    p.other_charges, p.guidance_value,
					    p.created_at AS p_created_at,
					    p.updated_at AS p_updated_at,
					    p.status,

					    f.project_file_id,
					    f.project_id AS f_project_id,
					    f.file_path AS f_file_path,
					    f.file_name AS f_file_name,
					    f.public_url AS f_public_url,
					    f.size_bytes AS f_size_bytes,

					    d.document_id,
					    d.project_id AS d_project_id,
					    d.document_type,
					    d.document_number,
					    d.file_name AS d_file_name,
					    d.file_path AS d_file_path,
					    d.uploaded_by,
					    d.uploaded_at

					FROM projects p
					LEFT JOIN projects_files f ON p.project_id = f.project_id
					LEFT JOIN project_documents d ON p.project_id = d.project_id

					WHERE p.status = 'ACTIVE'
					  AND p.project_id = ?

					ORDER BY p.created_at DESC;
					""";

			params = new Object[] { filter };
		}

		return jdbc.query(sql, params, projectExtractor);
	}

	private final ResultSetExtractor<List<ProjectSummaryDto>> projectExtractor = rs -> {

		Map<UUID, ProjectSummaryDto> projectMap = new LinkedHashMap<>();

		while (rs.next()) {

			UUID projectId = rs.getObject("project_id", UUID.class);
			ProjectSummaryDto project = projectMap.get(projectId);

			if (project == null) {
				project = new ProjectSummaryDto();
				project.setProjectId(projectId);
				project.setProjectName(rs.getString("project_name"));
				project.setLocationDetails(rs.getString("location_details"));
				project.setSurveyNumber(rs.getString("survey_number"));
				project.setStartDate(rs.getDate("start_date"));
				project.setEndDate(rs.getDate("end_date"));
				project.setPlotNumbers(rs.getString("plot_numbers"));
				project.setNoOfPlots(rs.getInt("no_of_plots"));
				project.setPlotStartNumber(rs.getInt("plot_start_number"));
				project.setPricePerSqft(rs.getBigDecimal("price_per_sqft"));
				project.setRegCharges(rs.getBigDecimal("reg_charges"));
				project.setDocCharges(rs.getBigDecimal("doc_charges"));
				project.setOtherCharges(rs.getBigDecimal("other_charges"));
				project.setGuidanceValue(rs.getBigDecimal("guidance_value"));

				Timestamp pCreatedAt = rs.getTimestamp("p_created_at");
				if (pCreatedAt != null) {
					project.setCreatedAt(pCreatedAt.toInstant());
				}

				Timestamp pUpdatedAt = rs.getTimestamp("p_updated_at");
				if (pUpdatedAt != null) {
					project.setUpdatedAt(pUpdatedAt.toInstant());
				}

				project.setStatus(rs.getString("status"));
				projectMap.put(projectId, project);
			}

			/* ---------- project_files ---------- */
			UUID fileId = rs.getObject("project_file_id", UUID.class);
			if (fileId != null) {
				boolean exists = project.getFiles().stream().anyMatch(f -> f.getProjectFileId().equals(fileId));

				if (!exists) {
					ProjectFileDto file = new ProjectFileDto();
					file.setProjectFileId(fileId);
					file.setProjectId(rs.getObject("f_project_id", UUID.class));
					file.setFilePath(rs.getString("f_file_path"));
					file.setFileName(rs.getString("f_file_name"));
					file.setPublicUrl(rs.getString("f_public_url"));
					file.setSizeByts(rs.getInt("f_size_bytes"));
					project.getFiles().add(file);
				}
			}

			/* ---------- project_documents ---------- */
			Long docId = rs.getLong("document_id");
			if (!rs.wasNull()) {
				boolean exists = project.getDocuments().stream().anyMatch(d -> d.getDocumentId().equals(docId));

				if (!exists) {
					ProjectDocumentDto doc = new ProjectDocumentDto();
					doc.setDocumentId(docId);
					doc.setProjectId(rs.getObject("d_project_id", UUID.class));
					doc.setDocumentType(rs.getString("document_type"));
					doc.setDocumentNumber(rs.getString("document_number"));
					doc.setFileName(rs.getString("d_file_name"));
					doc.setFilePath(rs.getString("d_file_path"));
					doc.setUploadedBy(rs.getObject("uploaded_by", UUID.class));
					doc.setUploadedAt(rs.getTimestamp("uploaded_at").toLocalDateTime());

					project.getDocuments().add(doc);
				}
			}
		}

		return new ArrayList<>(projectMap.values());
	};

}
