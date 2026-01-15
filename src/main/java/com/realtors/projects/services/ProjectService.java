package com.realtors.projects.services;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.realtors.admin.dto.form.DynamicFormResponseDto;
import com.realtors.admin.dto.form.EditResponseDto;
import com.realtors.admin.service.AbstractBaseService;
import com.realtors.common.EnumConstants;
import com.realtors.common.service.AuditContext;
import com.realtors.common.service.AuditTrailService;
import com.realtors.common.util.AppUtil;
import com.realtors.projects.dto.ProjectDetailDto;
import com.realtors.projects.dto.ProjectDto;
import com.realtors.projects.dto.ProjectSummaryDto;
import com.realtors.projects.repository.ProjectRepository;

@Service
public class ProjectService extends AbstractBaseService<ProjectDto, UUID> {

	private final JdbcTemplate jdbc;
	private final ProjectRepository repo;
	private final PlotUnitService plotService;
	private final AuditTrailService audit;

	public ProjectService(JdbcTemplate jdbc, ProjectRepository repo, PlotUnitService plotService,
			AuditTrailService audit) {
		super(ProjectDto.class, "projects", jdbc);
		this.jdbc = jdbc;
		this.repo = repo;
		this.plotService = plotService;
		this.audit = audit;
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
		audit.auditAsync("projects", opt.getProjectId(), EnumConstants.EDIT_FORM.toString(), AppUtil.getCurrentUserId(),
				AuditContext.getIpAddress(), AuditContext.getUserAgent());
		return new EditResponseDto<>(opt, form);
	}

	// this will get only active projects data
	public List<ProjectSummaryDto> getAciveProjects() {
		List<ProjectSummaryDto> projects = this.repo.getProjects(null);

		audit.auditAsync("projects", projects.getFirst().getProjectId(), EnumConstants.GET_ALL.toString(),
				AppUtil.getCurrentUserId(), AuditContext.getIpAddress(), AuditContext.getUserAgent());
		return projects;
	}

	// this is for Project Details Page where projects, files and plots details will
	// be served
	public ProjectDetailDto getProjectDetails(UUID projectId) {
		ProjectDetailDto dto = new ProjectDetailDto(this.repo.getProjects(projectId).getFirst(),
				this.plotService.getByProject(projectId), this.plotService.getPlotStat(projectId));
		audit.auditAsync("projects", projectId, EnumConstants.GET_ALL.toString(), AppUtil.getCurrentUserId(),
				AuditContext.getIpAddress(), AuditContext.getUserAgent());
		return dto;
	}

	// this will get all projects data irrespective of the status
	public List<ProjectDto> getAllProjects() {
		List<ProjectDto> list = super.findAllWithInactive();
		audit.auditAsync("projects", list.getFirst().getProjectId(), EnumConstants.GET_ALL.toString(),
				AppUtil.getCurrentUserId(), AuditContext.getIpAddress(), AuditContext.getUserAgent());
		return super.findAllWithInactive();
	}

	public Optional<ProjectDto> getProject(UUID id) {
		Optional<ProjectDto> opt = super.findById(id);
		audit.auditAsync("projects", opt.isPresent() ? opt.get().getProjectId() : null, EnumConstants.BY_ID.toString(),
				AppUtil.getCurrentUserId(), AuditContext.getIpAddress(), AuditContext.getUserAgent());
		return super.findById(id);
	}

	public ProjectDto createProject(ProjectDto dto) {
		ProjectDto data = super.create(dto);
		audit.auditAsync("projects", data.getProjectId(), EnumConstants.CREATE.toString(), AppUtil.getCurrentUserId(),
				AuditContext.getIpAddress(), AuditContext.getUserAgent());
		return data;
	}

	public ProjectDto updateProject(UUID id, ProjectDto dto) {
		ProjectDto data = super.update(id, dto);
		audit.auditAsync("projects", data.getProjectId(), EnumConstants.UPDATE.toString(), AppUtil.getCurrentUserId(),
				AuditContext.getIpAddress(), AuditContext.getUserAgent());
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

		audit.auditAsync("projects", data.getProjectId(), EnumConstants.PATCH.toString(), AppUtil.getCurrentUserId(),
				AuditContext.getIpAddress(), AuditContext.getUserAgent());
		return data;
	}

	public boolean deleteProject(UUID id) {
		audit.auditAsync("projects", id, EnumConstants.DELETE.toString(), AppUtil.getCurrentUserId(),
				AuditContext.getIpAddress(), AuditContext.getUserAgent());
		return super.softDelete(id);
	}
}
