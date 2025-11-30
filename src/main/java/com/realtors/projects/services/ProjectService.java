package com.realtors.projects.services;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.realtors.admin.dto.form.DynamicFormResponseDto;
import com.realtors.admin.dto.form.EditResponseDto;
import com.realtors.admin.service.AbstractBaseService;
import com.realtors.projects.dto.ProjectDetailDto;
import com.realtors.projects.dto.ProjectDto;
import com.realtors.projects.dto.ProjectSummaryDto;
import com.realtors.projects.repository.ProjectRepository;

@Service
public class ProjectService extends AbstractBaseService<ProjectDto, UUID> {

	private final JdbcTemplate jdbc;
	private final ProjectRepository repo;
	private final PlotUnitService plotService;
	public ProjectService(JdbcTemplate jdbc, ProjectRepository repo, PlotUnitService plotService) {
		super(ProjectDto.class, "projects", jdbc); 
		this.jdbc = jdbc;
		this.repo = repo;
		this.plotService = plotService;
	}
	
	@Override
	protected String getIdColumn() {
		return "project_id";
	}

	 /** ✅ Update user form response */
    public EditResponseDto<ProjectSummaryDto> editResponse(UUID projectId) {
    	logger.info("@ProjectService.getEditForm UUID id: "+projectId);
    	DynamicFormResponseDto form = super.buildDynamicFormResponse();
    	if (projectId ==null) {
    		return new EditResponseDto<>(null, form);
    	}
        ProjectSummaryDto opt = this.repo.getProjects(projectId).getFirst();
        return  new EditResponseDto<>(opt, form);
    }
	
	// this will get only active projects data 
	public List<ProjectSummaryDto> getAciveProjects() {
		List<ProjectSummaryDto> projects = this.repo.getProjects(null);
		return projects;
	}
	
	// this is for Project Details Page where projects, files and plots details will be served
	public ProjectDetailDto getProjectDetails(UUID projectId) {
		return new ProjectDetailDto(this.repo.getProjects(projectId).getFirst(), this.plotService.getByProject(projectId));
	}
	
	// this will get all projects data irrespective of the status
	public List<ProjectDto> getAllProjects() {
		return super.findAllWithInactive();
	}

	public Optional<ProjectDto> getProject(UUID id) {
		return super.findById(id);
	}

	public ProjectDto createProject(ProjectDto dto) {
		return super.create(dto);
	}

	public ProjectDto updateProject(UUID id, ProjectDto dto) {
		return super.update(id, dto);
	}
	
	public ProjectDto updatePatch(UUID id, Map<String, Object> dto) {
		return super.patch(id, dto);
	}

	public boolean deleteProject(UUID id) {
		return super.softDelete(id);
	}
}
