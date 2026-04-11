package com.realtors.projects.services;

import java.math.BigDecimal;
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

import com.realtors.admin.dto.form.DynamicFormResponseDto;
import com.realtors.admin.dto.form.EditResponseDto;
import com.realtors.admin.service.AbstractBaseService;
import com.realtors.common.EnumConstants;
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
		this.fileService = fileService;
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
		return super.findAllWithInactive();
	}

	public Optional<ProjectDto> getProject(UUID id) {
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
			dto.put("noOfPlots", plotNumbers);
		} else if (value != null && value instanceof String str) {
			// Case: JSON string -> split by comma
			plotNumbers = Arrays.stream(str.split(",")).map(String::trim) // remove extra spaces
					.filter(s -> !s.isEmpty()) // remove empty strings
					.toList();
			dto.put("noOfPlots", plotNumbers.size());
		}
		
		ProjectDto data = super.patch(projectId, dto);
		if (plotNumbers != null && !plotNumbers.isEmpty()) {
			plotService.syncPlots(projectId, plotNumbers);
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
		List<ProjectDocumentDto> list = jdbcTemplate.query("SELECT * FROM project_documents WHERE document_id = ?",
				new BeanPropertyRowMapper<>(ProjectDocumentDto.class), docId);
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

	@Transactional
	public void updateGuidelineAndRate(UUID projectId, BigDecimal guidanceValue, BigDecimal pricePerSqft) {

		// ✅ 1. If both null → exit early
		if (guidanceValue == null && pricePerSqft == null) {
			return; // no-op
		}
		// ✅ 2. Fetch existing values (only if needed)
		Map<String, Object> project = jdbcTemplate.queryForMap("""
				    SELECT guidance_value, price_per_sqft
				    FROM projects
				    WHERE project_id = ?
				""", projectId);

		BigDecimal existingGuidance = (BigDecimal) project.getOrDefault("guidance_value", BigDecimal.ZERO);
		BigDecimal existingRate = (BigDecimal) project.getOrDefault("price_per_sqft",  BigDecimal.ZERO);

		// ✅ 3. Decide final values
		BigDecimal finalGuidance = (guidanceValue != null) ? guidanceValue : existingGuidance;
		BigDecimal finalRate = (pricePerSqft != null) ? pricePerSqft : existingRate;

		// ✅ 4. Check if actually changed (optional but best practice)
		boolean isSame = finalGuidance.compareTo(existingGuidance) == 0 && finalRate.compareTo(existingRate) == 0;

		if (isSame) {
			return; // no change → skip DB update
		}

		// ✅ 5. Update project
		jdbcTemplate.update("""
				    UPDATE projects
				    SET
				        guidance_value = ?,
				        price_per_sqft = ?,
				        updated_at = now()
				    WHERE project_id = ?
				""", finalGuidance, finalRate, projectId);

		// ✅ 6. Update plots (same as before)
		jdbcTemplate.update("""
				    UPDATE plot_units pu
				    SET
				        base_price = pu.area * p.price_per_sqft,
				        total_price =
				            (pu.area * p.price_per_sqft)
				            +(p.reg_charges*p.guidance_value)/100
				            + p.doc_charges
				            + p.other_charges,
				        updated_at = now(),
				        rate_per_sqft=p.price_per_sqft,
				        registration_charges=(pu.area *p.reg_charges*p.guidance_value)/100
				    FROM projects p
				    WHERE pu.project_id = p.project_id
				      AND pu.project_id = ?
				      AND pu.status = 'AVAILABLE'
				""", projectId);
		audit.auditAsync("projects", projectId, EnumConstants.PRICE_UPDATE);
	}
}
