package com.realtors.projects.services;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.realtors.projects.controller.ProjectController;
import com.realtors.projects.dto.FileTemp;
import com.realtors.projects.dto.ProjectDto;
import com.realtors.projects.dto.ProjectFileDto;
import com.realtors.projects.dto.ProjectResponse;

import lombok.RequiredArgsConstructor;

//ProjectFacadeService.java
@Service
public class ProjectFacadeService {

	private final ProjectService projectService;
	private final ProjectFileService fileService;
	private final PlotUnitService plotService;
	private static final Logger logger = LoggerFactory.getLogger(ProjectFacadeService.class);
	
	public ProjectFacadeService(ProjectService projectService, ProjectFileService fileService, PlotUnitService plotService) {
		this.projectService = projectService;
		this.fileService = fileService;
		this.plotService = plotService;
	}

	/**
	 * The core transactional method. Use the txManager bean explicitly.
	 */
	@Transactional(transactionManager = "txManager")
	public ProjectResponse createProjectWithFilesAndPlots(ProjectDto dto, MultipartFile[] files) {
		
		// 1) create project (DB)
		ProjectDto created = projectService.createProject(dto);
		UUID project_id = created.getProjectId() ;
		logger.info("@ProjectFacadeServicecreateProjectWithFilesAndPlots project_id: "+project_id);
		// 2) save files to temp folder
		List<FileTemp> temps = fileService.saveFilesToTemp(project_id, files);

		try {
			// 3) insert file records (pointing to tempPath) INSIDE transaction
			fileService.insertFileRecordsAsTemp(temps);

			// 4) generate plots (inside same transaction)
			plotService.generatePlots(project_id, created.getNoOfPlots(), created.getPlotStartNumber());

			// 5) register afterCommit hook to move files to final folder and update DB
			fileService.registerAfterCommitMoveAndUpdate(temps);

			// 6) prepare response data (files read will currently show temp paths until
			// afterCommit moves)
			List<ProjectFileDto> fileDto = fileService.getProjectFiles(project_id);
			return new ProjectResponse(created, fileDto);
		} catch (RuntimeException ex) {
			// Cleanup temp files (we never moved to final), rethrow to trigger rollback
			fileService.cleanupTempFiles(temps);
			fileService.cleanupTempFolder(project_id);
			throw ex;
		} catch (Exception ex) {
			fileService.cleanupTempFiles(temps);
			fileService.cleanupTempFolder(project_id);
			throw new RuntimeException("Failed during project create orchestration", ex);
		} 
	}
}
