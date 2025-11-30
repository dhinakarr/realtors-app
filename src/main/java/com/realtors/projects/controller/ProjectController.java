package com.realtors.projects.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.realtors.admin.dto.form.EditResponseDto;
import com.realtors.common.ApiResponse;
import com.realtors.common.Utils;
import com.realtors.projects.dto.ProjectDetailDto;
import com.realtors.projects.dto.ProjectDto;
import com.realtors.projects.dto.ProjectFileDto;
import com.realtors.projects.dto.ProjectResponse;
import com.realtors.projects.dto.ProjectSummaryDto;
import com.realtors.projects.services.PlotUnitService;
import com.realtors.projects.services.ProjectFacadeService;
import com.realtors.projects.services.ProjectFileService;
import com.realtors.projects.services.ProjectService;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

	private final ProjectService service;
	private final ProjectFileService fileService;
	private final PlotUnitService plotService;
	private final ProjectFacadeService facade;

	private static final Logger logger = LoggerFactory.getLogger(ProjectController.class);

	public ProjectController(ProjectService service, ProjectFileService fileService, PlotUnitService plotService, ProjectFacadeService facade) {
		this.service = service;
		this.fileService = fileService;
		this.plotService = plotService;
		this.facade = facade;
	}

	@GetMapping("/form")
	public ResponseEntity<ApiResponse<EditResponseDto<ProjectSummaryDto>>> getForm() {
		EditResponseDto<ProjectSummaryDto> projects = service.editResponse(null);
		return ResponseEntity.ok(ApiResponse.success("Users fetched successfully", projects, HttpStatus.OK));
	}

	@GetMapping("/form/{id}")
	public ResponseEntity<ApiResponse<EditResponseDto<ProjectSummaryDto>>> getEditForm(@PathVariable UUID id) {
//		logger.info("@ProjectController.getEditForm UUID id: "+id);
		EditResponseDto<ProjectSummaryDto> projects = service.editResponse(id);
		return ResponseEntity.ok(ApiResponse.success("Users fetched successfully", projects, HttpStatus.OK));
	}

	@GetMapping
	public ResponseEntity<ApiResponse<List<ProjectSummaryDto>>> getAll() {
		List<ProjectSummaryDto> projects = service.getAciveProjects(); // active projects only
		return ResponseEntity.ok(ApiResponse.success("Projects Fetched", projects, HttpStatus.OK));
	}
	
	@GetMapping("/details/{id}")
	public ResponseEntity<ApiResponse<ProjectDetailDto>> getProjectDetails(@PathVariable UUID id) {
		ProjectDetailDto projects = service.getProjectDetails(id); // active projects only
		return ResponseEntity.ok(ApiResponse.success("Projects Fetched", projects, HttpStatus.OK));
	}

	@GetMapping("/{id}")
	public ResponseEntity<ApiResponse<Optional<ProjectDto>>> getOne(@PathVariable UUID id) {
		Optional<ProjectDto> dto = service.getProject(id);
		return ResponseEntity.ok(ApiResponse.success("Projects Fetched", dto, HttpStatus.OK));
	}

	@PostMapping(value = "/all", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<ApiResponse<ProjectResponse>> createProject(@ModelAttribute ProjectDto dto,
			@RequestPart(value = "files", required = false) MultipartFile[] files) {

		/*
		 * ProjectDto created = service.createProject(dto);
		 * logger.info("@ProjectController.createProject file created: ");
		 * uploadFiles(created.getProjectId(), files);
		 * logger.info("@ProjectController.createProject file uploaded: ");
		 * List<ProjectFileDto> fileDto =
		 * fileService.getProjectFiles(created.getProjectId()); ProjectResponse response
		 * = new ProjectResponse(dto, fileDto);
		 * plotService.generatePlots(created.getProjectId(), created.getNoOfPlots(),
		 * created.getPlotStartNumber());
		 */			
		  ProjectResponse response = facade.createProjectWithFilesAndPlots(dto, files);
		  return ResponseEntity.ok(ApiResponse.success("Projects Fetched", response, HttpStatus.OK));
	}

	@PostMapping
	public ResponseEntity<ApiResponse<ProjectDto>> create(@RequestBody ProjectDto dto) {
		ProjectDto data = service.createProject(dto);
		return ResponseEntity.ok(ApiResponse.success("Project created", data, HttpStatus.OK));
	}

	@PutMapping("/{id}")
	public ResponseEntity<String> update(@PathVariable UUID id, @RequestBody ProjectDto dto) {
		dto.setProjectId(id);
		ProjectDto updated = service.updateProject(id, dto);
		return ResponseEntity.ok("Project updated");
	}

	@DeleteMapping("/file/delete/{fileId}")
	public ResponseEntity<ApiResponse<?>> deleteFile(@PathVariable String fileId,
	        @RequestParam String projectId) {
		logger.info("@ProjectController.deleteFile fileId:  "+fileId);
		UUID file = UUID.fromString(fileId);
		UUID project = UUID.fromString(projectId);
		boolean deleted = fileService.deleteFile(file);
		if (deleted) 
			return ResponseEntity.ok(ApiResponse.success("File deleted", null, HttpStatus.OK));
		else 
			return ResponseEntity.badRequest().body(ApiResponse.failure("Failed to delete file"));
	}
	
	@DeleteMapping("/{id}")
	public ResponseEntity<?> delete(@PathVariable UUID id) {
		boolean deleted = service.deleteProject(id);
		if (deleted)
			return ResponseEntity.ok(ApiResponse.success("Project deleted", null));
		return ResponseEntity.badRequest().body(ApiResponse.failure("Project deleted"));
	}

	@PatchMapping(path = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<ApiResponse<ProjectDto>> patch(@PathVariable UUID id,
			@RequestPart(value = "files", required = false) MultipartFile[] files,
			@RequestParam Map<String, Object> otherFields) { // This contains all other fields
		
		Map<String, Object> updates = new HashMap<String, Object>();
		ProjectDto updated = new ProjectDto();
		if (otherFields != null && !otherFields.isEmpty()) {
			Set<String> integerFields = Set.of("noOfPlots", "plotStartNumber", "pricePerSqft", "regCharges", "docCharges", "otherCharges", "guidanceValue"); 
			updates = Utils.castNumberFields(otherFields, integerFields);
//			updates = castNumberFields(otherFields);
		}
		if (updates.isEmpty()) {
			updated = service.getProject(id).get();
		} else {
			updated = service.updatePatch(id, updates);
		}
		uploadFiles(updated.getProjectId(), files);
		return ResponseEntity.ok(ApiResponse.success("Updated successfully", updated, HttpStatus.OK));
	}
	
	@GetMapping("/file/{fileId}")
	public ResponseEntity<Resource> serveFile(@PathVariable UUID fileId) throws IOException {
	    ProjectFileDto file = fileService.getFileById(fileId);
	    if (file == null) {
	        return ResponseEntity.notFound().build();
	    }
	    Path path = Paths.get(file.getFilePath());
	    if (!Files.exists(path)) {
	        return ResponseEntity.notFound().build();
	    }

	    Resource resource = new UrlResource(path.toUri());
	    return ResponseEntity.ok()
	            .contentType(getMediaType(path))
	            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + file.getFileName() + "\"")
	            .body(resource);
	}

	private MediaType getMediaType(Path path) {
	    try {
	        String mime = Files.probeContentType(path);
	        return mime != null ? MediaType.parseMediaType(mime) : MediaType.APPLICATION_OCTET_STREAM;
	    } catch (IOException e) {
	        return MediaType.APPLICATION_OCTET_STREAM;
	    }
	}
	
	private boolean uploadFiles(UUID projectId, MultipartFile[] files) {
		try {
			if (files != null) {
				fileService.uploadMultipleFiles(projectId, files);
			}
			logger.info("@ProjectController.uploadFiles image inserted successfully ");
			return true;
		} catch (Exception e) {
			logger.error("@ProjectController.uploadFiles Failed to insert data: " + e.getMessage());
			return false;
		}
	}
}
