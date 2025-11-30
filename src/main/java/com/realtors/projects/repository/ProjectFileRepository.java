package com.realtors.projects.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.realtors.common.util.AppUtil;
import com.realtors.projects.dto.ProjectFileDto;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ProjectFileRepository {

	private final JdbcTemplate jdbc;

	public void saveFile(UUID projectId, String filePath, String fileName, long size) {
		String sql = """
				    INSERT INTO projects_files (project_file_id, project_id, file_path, file_name, size_bytes)
				    VALUES (?, ?, ?, ?, ?)
				""";
		jdbc.update(sql, UUID.randomUUID(), projectId, filePath, fileName, size);
	}

	// this helps to store the file data into database
	public void saveFileData(ProjectFileDto dto) {
		String sql = """
				    INSERT INTO projects_files (project_file_id, project_id, file_path, file_name, public_url, size_bytes, created_by, updated_by)
				    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
				""";
		UUID userId = AppUtil.getCurrentUserId();
		jdbc.update(sql, dto.getProjectFileId(), dto.getProjectId(), dto.getFilePath(), dto.getFileName(),
				dto.getPublicUrl(), dto.getSizeByts(), userId, userId);
	}

	// this helps to fetch the list of file data from database by Project Id
	public List<ProjectFileDto> findByProjectId(UUID projectId) {
		String sql = "SELECT * FROM projects_files WHERE project_id = ?";
		return jdbc.query(sql, new ProjectFileRowMapper(), projectId);
	}

	// this helps to fetch the file data from database by File Id
	public ProjectFileDto findByProjectFileId(UUID fileId) {
		String sql = "SELECT * FROM projects_files WHERE project_file_id = ?";
		return jdbc.query(sql, new ProjectFileRowMapper(), fileId).getFirst();
	}

	// this helps to delete the file data from db
	public boolean deleteFile(UUID fileId) {
		String sql = "DELETE FROM projects_files WHERE project_file_id = ?";
		int count = jdbc.update(sql, fileId);
		return count > 0 ? true : false;
	}
}
