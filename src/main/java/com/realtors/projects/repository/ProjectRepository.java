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
import com.realtors.projects.controller.ProjectController;
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
	             start_date, end_date, no_of_plots, price_per_sqft, reg_charges,
	             doc_charges, other_charges, guidance_value, status, 
	             created_by, updated_by, created_at, updated_at)
	            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING *;
	            """;
	        UUID userId = AppUtil.getCurrentUserId();
	        // 3. Prepare the value array, ensuring all fields are included in order
	        Object[] args = new Object[] {
	            dto.getProjectName(),
	            dto.getLocationDetails(),
	            dto.getSurveyNumber(),
	            dto.getStartDate(),
	            dto.getEndDate(),
	            dto.getNoOfPlots(),
	            dto.getPricePerSqft(),
	            dto.getRegCharges(),
	            dto.getDocCharges(),
	            dto.getOtherCharges(),
	            dto.getGuidanceValue(),
	            dto.getStatus(), // The missing 'status' field
	            userId, userId, OffsetDateTime.now(), OffsetDateTime.now()
	        };
	        
	        // 4. Use queryForObject with the RowMapper to return the inserted DTO
	        return jdbc.queryForObject(sql, args, rowMapper);
	}

	public int update(ProjectDto dto) {
		String sql = """
				    UPDATE projects SET
				      project_name=?, location_details=?, survey_number=?, start_date=?,
				      end_date=?, no_of_plots=?, price_per_sqft=?, reg_charges=?,
				      doc_charges=?, other_charges=?, guidance_value=?, images_location=?,
				      meta=?, updated_at=NOW()
				    WHERE project_id=?;
				""";

		return jdbc.update(sql, dto.getProjectName(), dto.getLocationDetails(), dto.getSurveyNumber(),
				dto.getStartDate(), dto.getEndDate(), dto.getNoOfPlots(), dto.getPricePerSqft(), dto.getRegCharges(),
				dto.getDocCharges(), dto.getOtherCharges(), dto.getGuidanceValue());
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
                    p.project_id, p.project_name, p.location_details, p.survey_number, p.start_date, p.end_date, 
                    p.no_of_plots, p.plot_start_number, p.price_per_sqft, p.reg_charges, p.doc_charges, 
                    p.other_charges, p.guidance_value, p.created_at AS p_created_at, p.updated_at AS p_updated_at, 
                    p.status, p.created_by, p.updated_by,
                    f.project_file_id, f.project_id AS f_project_id, f.file_path, f.file_name, f.public_url, f.size_bytes, 
                    f.created_at AS f_created_at, f.updated_at AS f_updated_at, f.created_by AS f_created_by, 
                    f.updated_by AS f_updated_by
                FROM projects p
                LEFT JOIN (
                    SELECT *, ROW_NUMBER() OVER (PARTITION BY project_id ORDER BY created_at ASC) AS rn
                    FROM projects_files
                ) f ON p.project_id = f.project_id AND f.rn = 1
                WHERE p.status = 'ACTIVE'
                ORDER BY p.created_at DESC, f.created_at ASC;
                """;
            params = new Object[]{};
        } else {
            // With filter: primary + all secondary
            sql = """
                SELECT 
                    p.project_id, p.project_name, p.location_details, p.survey_number, p.start_date, p.end_date, 
                    p.no_of_plots, p.plot_start_number, p.price_per_sqft, p.reg_charges, p.doc_charges, 
                    p.other_charges, p.guidance_value, p.created_at AS p_created_at, p.updated_at AS p_updated_at, 
                    p.status, p.created_by, p.updated_by,
                    f.project_file_id, f.project_id AS f_project_id, f.file_path, f.file_name, f.public_url, f.size_bytes, 
                    f.created_at AS f_created_at, f.updated_at AS f_updated_at, f.created_by AS f_created_by, 
                    f.updated_by AS f_updated_by
                FROM projects p
                LEFT JOIN projects_files f ON p.project_id = f.project_id
                WHERE p.status = 'ACTIVE' AND p.project_id=?
                ORDER BY p.created_at DESC, f.created_at ASC;
                """;
            params = new Object[]{filter};
        }
        List<ProjectSummaryDto> list = jdbc.query(sql, params, projectExtractor);
        logger.info("@ProjectRepository.getProjects publicUrl: "+list.getFirst().getFiles().getFirst().getPublicUrl());
        return list;
    }
    
    private final ResultSetExtractor<List<ProjectSummaryDto>> projectExtractor = new ResultSetExtractor<>() {
        @Override
        public List<ProjectSummaryDto> extractData(ResultSet rs) throws SQLException {
            Map<UUID, ProjectSummaryDto> projectMap = new LinkedHashMap<>(); // Preserves insertion order
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

                UUID fileId = rs.getObject("project_file_id", UUID.class);
                if (fileId != null) {
                    ProjectFileDto file = new ProjectFileDto();
                    file.setProjectFileId(fileId);
                    file.setProjectId(rs.getObject("f_project_id", UUID.class));
                    file.setFilePath(rs.getString("file_path"));
                    file.setFileName(rs.getString("file_name"));
                    file.setPublicUrl(rs.getString("public_url"));
                    file.setSizeByts(rs.getInt("size_bytes"));
                    Timestamp fCreatedAt = rs.getTimestamp("f_created_at");
                    project.getFiles().add(file);
                }
            }
            return new ArrayList<>(projectMap.values());
        }
    };
}
