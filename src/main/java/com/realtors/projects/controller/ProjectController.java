package com.realtors.projects.controller;

import java.io.IOException;
import java.io.InputStream;
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
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRange;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.realtors.admin.dto.form.EditResponseDto;
import com.realtors.common.ApiResponse;
import com.realtors.common.Utils;
import com.realtors.projects.dto.ProjectDetailDto;
import com.realtors.projects.dto.ProjectDocumentDto;
import com.realtors.projects.dto.ProjectDto;
import com.realtors.projects.dto.ProjectFileDto;
import com.realtors.projects.dto.ProjectResponse;
import com.realtors.projects.dto.ProjectSummaryDto;
import com.realtors.projects.services.ProjectFacadeService;
import com.realtors.projects.services.ProjectFileService;
import com.realtors.projects.services.ProjectService;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {
	private static final Logger logger = LoggerFactory.getLogger(ProjectController.class);
	private final ProjectService service;
	private final ProjectFileService fileService;
	private final ProjectFacadeService facade;

	public ProjectController(ProjectService service, ProjectFileService fileService, ProjectFacadeService facade) {
		this.service = service;
		this.fileService = fileService;
		this.facade = facade;
	}

	@GetMapping("/form")
	public ResponseEntity<ApiResponse<EditResponseDto<ProjectSummaryDto>>> getForm() {
		EditResponseDto<ProjectSummaryDto> projects = service.editResponse(null);
		return ResponseEntity.ok(ApiResponse.success("Projects fetched successfully", projects, HttpStatus.OK));
	}

	@GetMapping("/form/{id}")
	public ResponseEntity<ApiResponse<EditResponseDto<ProjectSummaryDto>>> getEditForm(@PathVariable UUID id) {
//		logger.info("@ProjectController.getEditForm UUID id: "+id);
		EditResponseDto<ProjectSummaryDto> projects = service.editResponse(id);
		return ResponseEntity.ok(ApiResponse.success("Projects fetched successfully", projects, HttpStatus.OK));
	}

	@GetMapping
	public ResponseEntity<ApiResponse<List<ProjectSummaryDto>>> getAll() {
		List<ProjectSummaryDto> projects = service.getAciveProjects(); // active projects only

		return ResponseEntity.ok(ApiResponse.success("Projects Fetched", projects, HttpStatus.OK));
	}

	@GetMapping("/details/{id}")
	public ResponseEntity<ApiResponse<ProjectDetailDto>> getProjectDetails(@PathVariable String id) {
		UUID projectId = UUID.fromString(id);
		ProjectDetailDto projects = service.getProjectDetails(projectId); // active projects only
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

		ProjectResponse response = facade.createProjectWithFilesAndPlots(dto, files);
		return ResponseEntity.ok(ApiResponse.success("Projects Fetched", response, HttpStatus.OK));
	}

	@DeleteMapping("/file/delete/{fileId}")
	public ResponseEntity<ApiResponse<?>> deleteFile(@PathVariable String fileId) {
		UUID file = UUID.fromString(fileId);
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
			Set<String> integerFields = Set.of("noOfPlots", "plotStartNumber", "pricePerSqft", "regCharges",
					"docCharges", "otherCharges", "guidanceValue");
			updates = Utils.castNumberFields(otherFields, integerFields);
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
		logger.info("@ProjectController.serveFile /file/{fileId}: {}", fileId);
		ProjectFileDto file = fileService.getFileById(fileId);
		if (file == null) {
			return ResponseEntity.notFound().build();
		}
		Path path = Paths.get(file.getFilePath());
		logger.info("@ProjectController.serveFile path: {}", path.toString());
		if (!Files.exists(path)) {
			return ResponseEntity.notFound().build();
		}
		Resource resource = new UrlResource(path.toUri());
		logger.info("@ProjectController.serveFile resource: {}", resource.toString());
		return ResponseEntity.ok().contentType(getMediaType(path))
				.header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + file.getFileName() + "\"")
				.body(resource);
	}

	private MediaType getMediaType(Path path) {
		try {
			String mime = Files.probeContentType(path);
			logger.info("@ProjectController.getMediaType mime: {}", mime);
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
			return true;
		} catch (Exception e) {
			logger.error("@ProjectController.uploadFiles Failed to insert data: " + e.getMessage());
			return false;
		}
	}

	@PostMapping(value = "/{projectId}/documents", consumes = { "multipart/form-data" })
	public ResponseEntity<ApiResponse<?>> uploadDocument(@PathVariable UUID projectId,
			@RequestParam String documentType, @RequestParam String documentNumber, @RequestParam MultipartFile files)
			throws Exception {
		service.uploadDocument(projectId, documentType, documentNumber, files);
		return ResponseEntity.ok(ApiResponse.success("Document uploaded", null));
	}

	@GetMapping("/{projectId}/documents")
	public ResponseEntity<ApiResponse<List<ProjectDocumentDto>>> getDocuments(@PathVariable UUID projectId) {
		if (projectId == null)
			return ResponseEntity.badRequest().body(ApiResponse.failure("Project Id is mandatory", null));

		List<ProjectDocumentDto> list = service.findDocumentsByUserId(projectId);
		return ResponseEntity.ok(ApiResponse.success("User documents fetched", list));
	}

	@DeleteMapping(value = "/documents/{docId}", produces = "application/json")
	public ResponseEntity<ApiResponse<?>> deleteDocument(@PathVariable String docId) {
		if (docId == null || docId.isBlank()) {
			return ResponseEntity.badRequest().body(ApiResponse.failure("Document Id is mandatory", null));
		}
		Long id;
		try {
			id = Long.parseLong(docId);
		} catch (NumberFormatException e) {
			return ResponseEntity.badRequest().body(ApiResponse.failure("Invalid Document Id", null));
		}

		service.deleteDocument(id);
		logger.info("@UserController.getDocuments document deleted");
		return ResponseEntity.ok(ApiResponse.success("Document deleted", null));
	}

	@GetMapping("/documents/{docId}/download")
	public ResponseEntity<Resource> downloadDocument(@PathVariable Long docId) throws IOException {
		ProjectDocumentDto doc = service.findByDocumentId(docId);
		if (doc == null) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
		}

		Path path = Paths.get(doc.getFilePath());
		if (!Files.exists(path)) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
		}
		Resource resource = new UrlResource(path.toUri());
		String contentType = Files.probeContentType(path);
		if (contentType == null) {
			contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
		}

		boolean inline = contentType.startsWith("image/") || contentType.startsWith("video/")
				|| contentType.startsWith("audio/") || contentType.equals("application/pdf");

		String disposition = inline ? "inline; filename=\"" + doc.getFileName() + "\""
				: "attachment; filename=\"" + doc.getFileName() + "\"";

		return ResponseEntity.ok().contentType(MediaType.parseMediaType(contentType))
				.header(HttpHeaders.CONTENT_DISPOSITION, disposition).body(resource);
	}

	@GetMapping("/documents/{id}/view")
	public ResponseEntity<ResourceRegion> viewDocument(@PathVariable Long id,
			@RequestHeader(value = HttpHeaders.RANGE, required = false) String rangeHeader) throws IOException {

		ProjectDocumentDto doc = service.findByDocumentId(id);
		if (doc == null) {
			return ResponseEntity.notFound().build();
		}

		Path path = Paths.get(doc.getFilePath());
		if (!Files.exists(path)) {
			return ResponseEntity.notFound().build();
		}

		UrlResource resource = new UrlResource(path.toUri());
		long contentLength = Files.size(path);
		MediaType mediaType = getMediaType(path);

		// ✅ Default chunk size (1MB)
		long chunkSize = 1024 * 1024;

		if (rangeHeader == null) {
			// No range → send first chunk
			ResourceRegion region = new ResourceRegion(resource, 0, Math.min(chunkSize, contentLength));
			return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT).contentType(mediaType).body(region);
		}

		HttpRange range = HttpRange.parseRanges(rangeHeader).get(0);
		long start = range.getRangeStart(contentLength);
		long end = range.getRangeEnd(contentLength);
		long rangeLength = Math.min(chunkSize, end - start + 1);

		ResourceRegion region = new ResourceRegion(resource, start, rangeLength);

		return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT).contentType(mediaType).body(region);
	}

}
