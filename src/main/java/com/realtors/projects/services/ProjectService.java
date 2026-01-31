package com.realtors.projects.services;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.realtors.admin.dto.UserDocumentDto;
import com.realtors.admin.dto.form.DynamicFormResponseDto;
import com.realtors.admin.dto.form.EditResponseDto;
import com.realtors.admin.service.AbstractBaseService;
import com.realtors.common.EnumConstants;
import com.realtors.common.service.AuditContext;
import com.realtors.common.service.AuditTrailService;
import com.realtors.common.service.FileSavingService;
import com.realtors.common.service.FileStorageContext;
import com.realtors.common.util.AppUtil;
import com.realtors.projects.dto.ProjectDetailDto;
import com.realtors.projects.dto.ProjectDocumentDto;
import com.realtors.projects.dto.ProjectDto;
import com.realtors.projects.dto.ProjectSummaryDto;
import com.realtors.projects.repository.ProjectRepository;

@Service
public class ProjectService extends AbstractBaseService<ProjectDto, UUID> {

	private final JdbcTemplate jdbc;
	private final ProjectRepository repo;
	private final PlotUnitService plotService;
	private final AuditTrailService audit;
	private final FileSavingService fileService;

	public ProjectService(JdbcTemplate jdbc, ProjectRepository repo, PlotUnitService plotService,
			AuditTrailService audit, FileSavingService fileService) {
		super(ProjectDto.class, "projects", jdbc);
		this.jdbc = jdbc;
		this.repo = repo;
		this.plotService = plotService;
		this.audit = audit;
		this.fileService= fileService; 
	}

	@Override
	protected String getIdColumn() {
		return "project_id";
	}

	/** ✅ Update user form response */
	public EditResponseDto<ProjectSummaryDto> editResponse(UUID projectId) {
		DynamicFormResponseDto form = super.buildDynamicFormResponse();
		if (projectId == null) {
			return new EditResponseDto<>(null, form);
		}
		ProjectSummaryDto opt = this.repo.getProjects(projectId).getFirst();
		return new EditResponseDto<>(opt, form);
	}

	// this will get only active projects data
	public List<ProjectSummaryDto> getAciveProjects() {
		List<ProjectSummaryDto> projects = this.repo.getProjects(null);

		return projects;
	}

	// this is for Project Details Page where projects, files and plots details will
	// be served
	public ProjectDetailDto getProjectDetails(UUID projectId) {
		ProjectDetailDto dto = new ProjectDetailDto(this.repo.getProjects(projectId).getFirst(),
				this.plotService.getByProject(projectId), this.plotService.getPlotStat(projectId));
		return dto;
	}

	// this will get all projects data irrespective of the status
	public List<ProjectDto> getAllProjects() {
		List<ProjectDto> list = super.findAllWithInactive();
		return super.findAllWithInactive();
	}

	public Optional<ProjectDto> getProject(UUID id) {
		Optional<ProjectDto> opt = super.findById(id);
		return super.findById(id);
	}

	public ProjectDto createProject(ProjectDto dto) {
		ProjectDto data = super.create(dto);
		audit.auditAsync("projects", data.getProjectId(), EnumConstants.CREATE);
		return data;
	}

	public ProjectDto updateProject(UUID id, ProjectDto dto) {
		ProjectDto data = super.update(id, dto);
		audit.auditAsync("projects", data.getProjectId(), EnumConstants.UPDATE);
		return data;
	}

	public ProjectDto updatePatch(UUID projectId, Map<String, Object> dto) {
		Object value = dto.get("plotNumbers");
		List<String> plotNumbers = null;

		if (value != null && value instanceof List<?> list) {
			// Case: JSON array -> List
			plotNumbers = list.stream().filter(Objects::nonNull).map(Object::toString).toList();
		} else if (value != null && value instanceof String str) {
			// Case: JSON string -> split by comma
			plotNumbers = Arrays.stream(str.split(",")).map(String::trim) // remove extra spaces
					.filter(s -> !s.isEmpty()) // remove empty strings
					.toList();
		}
		ProjectDto data = super.patch(projectId, dto);
		if (plotNumbers != null && !plotNumbers.isEmpty()) {
			plotService.deleteByProjectId(projectId);
			plotService.generatePlots(projectId, plotNumbers);
		}

		audit.auditAsync("projects", data.getProjectId(), EnumConstants.PATCH);
		return data;
	}

	public boolean deleteProject(UUID id) {
		audit.auditAsync("projects", id, EnumConstants.DELETE);
		return super.softDelete(id);
	}
	
	
	@Transactional("txManager")
	public void uploadDocument(UUID projectId, String documentType, String documentNumber, MultipartFile file)
			throws Exception {

		if (file == null) {
			throw new IllegalArgumentException("No files uploaded");
		}
		FileStorageContext ctx = new FileStorageContext(file, projectId, "projects", "/documents/");
		String imagePathUrl = fileService.saveFile(ctx);

		ProjectDocumentDto docDto = new ProjectDocumentDto();
		docDto.setProjectId(projectId);
		docDto.setDocumentNumber(documentNumber);
		docDto.setDocumentType(documentType);
		docDto.setFileName(file.getOriginalFilename());
		docDto.setFilePath(imagePathUrl);
		docDto.setUploadedAt(LocalDateTime.now());
		docDto.setUploadedBy(AppUtil.getCurrentUserId());
		audit.auditAsync("projects", projectId, EnumConstants.UPDATE_DOCUMENT);
		save(docDto);
	}

	private void save(ProjectDocumentDto d) {
		String sql = """
				    INSERT INTO project_documents
				    (project_id, document_type, document_number, file_name, file_path, uploaded_at, uploaded_by)
				    VALUES (?, ?, ?, ?, ?, ?, ?)
				""";
		jdbcTemplate.update(sql, d.getProjectId(), d.getDocumentType(), d.getDocumentNumber(), d.getFileName(),
				d.getFilePath(), d.getUploadedAt(), d.getUploadedBy());
	}

	public ProjectDocumentDto findByDocumentId(Long docId) {
		List<ProjectDocumentDto> list = jdbcTemplate.query(
		        "SELECT * FROM project_documents WHERE document_id = ?",
		        new BeanPropertyRowMapper<>(ProjectDocumentDto.class),
		        docId
		    );
		    return list.isEmpty() ? null : list.get(0);
	}
	
	public void deleteDocument(Long docId) {
		ProjectDocumentDto dto = findByDocumentId(docId);
		UUID projectId = dto.getProjectId();
		fileService.deleteDocument(projectId, "projects", "/documents/", dto.getFileName());
		audit.auditAsync("projects", projectId, EnumConstants.DELETE_DOCUMENT);
		// delete DB record
		delete(docId);
	}
	
	private void delete(Long docId) {
		jdbcTemplate.update("DELETE FROM project_documents WHERE document_id = ?", docId);
	}
	
	public List<ProjectDocumentDto> findDocumentsByUserId(UUID userId) {
		String sql = "SELECT *  FROM project_documents WHERE project_id = ?";
		return jdbcTemplate.query(sql, new Object[] { userId }, (rs, rowNum) -> {
			ProjectDocumentDto d = new ProjectDocumentDto();
			d.setDocumentId(rs.getLong("document_id"));
			d.setProjectId(UUID.fromString(rs.getString("project_id")));
			d.setDocumentType(rs.getString("document_type"));
			d.setDocumentNumber(rs.getString("document_number"));
			d.setFileName(rs.getString("file_name"));
			d.setFilePath(rs.getString("file_path"));
			d.setUploadedBy(rs.getString("uploaded_by") != null ? UUID.fromString(rs.getString("uploaded_by")) : null);
			d.setUploadedAt(rs.getTimestamp("uploaded_at").toLocalDateTime());
			return d;
		});
	}
}
