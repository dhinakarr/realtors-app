package com.realtors.projects.services;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.realtors.common.config.FileStorageProperties;
import com.realtors.projects.dto.FileTemp;
import com.realtors.projects.dto.ProjectFileDto;
import com.realtors.projects.repository.ProjectFileRepository;

@Service
public class ProjectFileService {

	private final FileStorageService storage;
	private final ProjectFileRepository repo;
	private final FileStorageProperties props;
	private final JdbcTemplate jdbc;
	private static final Logger logger = LoggerFactory.getLogger(FileStorageService.class);

	public ProjectFileService(FileStorageService storage, ProjectFileRepository repo, FileStorageProperties props,
			JdbcTemplate jdbc) {
		this.storage = storage;
		this.repo = repo;
		this.props = props;
		this.jdbc = jdbc;
	}

	public void uploadMultipleFiles(UUID projectId, MultipartFile[] files) throws IOException {
		for (MultipartFile f : files) {
			uploadSingleFile(projectId, f);
		}
	}

	private Path getUploadRoot() {
		return Paths.get(props.getUploadDir());
	}

	private Path getTempFolder(UUID projectId) {
		Path folder = getUploadRoot().resolve(props.getTempDir()).resolve(projectId.toString());
		try {
			Files.createDirectories(folder);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return folder;
	}

	private Path getFinalFolder(UUID projectId) {
		Path folder = getUploadRoot().resolve("projects").resolve(projectId.toString());
		try {
			Files.createDirectories(folder);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return folder;
	}

	public void uploadSingleFile(UUID projectId, MultipartFile file) throws IOException {

//		String fullPath = storage.saveProjectFile(projectId, file);
		ProjectFileDto dto = storage.storeFile(projectId, file);
//		repo.saveFile(projectId, fullPath, file.getOriginalFilename(), file.getSize());
		repo.saveFileData(dto);
	}

	public List<ProjectFileDto> getProjectFiles(UUID projectId) {
		List<ProjectFileDto> list = repo.findByProjectId(projectId);
		logger.info("@ProjectFileService.getProjectFiles fileDto.getFilePath(): " + list.getFirst().getProjectFileId());
		return list;
	}

	public ProjectFileDto getFileById(UUID fileId) {
		return repo.findByProjectFileId(fileId);
	}

	public boolean deleteFile(UUID fileId) {
		ProjectFileDto fileDto = repo.findByProjectFileId(fileId);
		boolean flag = false;
		logger.info("@ProjectFileService.deleteFile fileDto.getFilePath(): " + fileDto.getFilePath());
		boolean fileDeleted = storage.deleteFile(fileDto.getFilePath());
//		Path fileFolder = getFinalFolder(fileDto.getProjectId()); // this is to consider  deleting the folder if required
		
		boolean dataDeleted = repo.deleteFile(fileId);
		if (fileDeleted && dataDeleted)
			flag = true;
		logger.info("ProjectFileService.deleteFile File& Data Deleted: " + flag);
		return flag;
	}

	public Resource getFile(UUID fileId) throws IOException {
		ProjectFileDto dto = repo.findByProjectFileId(fileId);
		Path path = Paths.get(dto.getFilePath());
		return new UrlResource(path.toUri());
	}

	public boolean uploadFiles(UUID projectId, MultipartFile[] files) {
		try {
			if (files != null) {
				uploadMultipleFiles(projectId, files);
			}
			logger.info("@ProjectController.uploadFiles image inserted successfully ");
			return true;
		} catch (Exception e) {
			logger.error("@ProjectController.uploadFiles Failed to insert data: " + e.getMessage());
			return false;
		}
	}

	/**
	 * Save incoming files to a temp folder and return list of FileTemp metadata.
	 */
	public List<FileTemp> saveFilesToTemp(UUID projectId, MultipartFile[] files) {
		if (files == null || files.length == 0)
			return Collections.emptyList();

		Path tempFolder = getTempFolder(projectId);
		List<FileTemp> out = new ArrayList<>();

		for (MultipartFile mf : files) {
			try {
				UUID fileId = UUID.randomUUID();
				String uniqueName = System.currentTimeMillis() + "_" + Objects.requireNonNull(mf.getOriginalFilename());
				Path tmp = tempFolder.resolve(uniqueName);
				try (InputStream in = mf.getInputStream()) {
					Files.copy(in, tmp, StandardCopyOption.REPLACE_EXISTING);
				}

				Path finalPath = getFinalFolder(projectId).resolve(uniqueName);
				FileTemp ft = new FileTemp();
				ft.setId(fileId);
				ft.setProjectId(projectId);
				ft.setOriginalFileName(mf.getOriginalFilename());
				ft.setTempPath(tmp);
				ft.setFinalPath(finalPath);
				out.add(ft);
			} catch (IOException e) {
				// On error during write, clean up already created temp files for this call
				out.forEach(f -> {
					try {
						Files.deleteIfExists(f.getTempPath());
					} catch (IOException ignored) {
					}
				});
				throw new RuntimeException("Failed to save upload to temp", e);
			}
		}
		return out;
	}

	/**
	 * Insert DB records for files that currently live in temp (within transaction).
	 * We store file_path initially as the tempPath (so that if transaction rolls
	 * back, no final path is left pointing to non-existing file). public_url can be
	 * constructed from fileId.
	 */
	public void insertFileRecordsAsTemp(List<FileTemp> temps) {
		if (temps == null || temps.isEmpty())
			return;

		String sql = "INSERT INTO projects_files (project_file_id, project_id, file_path, public_url, file_name, created_at) "
				+ "VALUES (?, ?, ?, ?, ?, current_timestamp)";

		for (FileTemp f : temps) {
			String publicUrl = ServletUriComponentsBuilder.fromCurrentContextPath().path("/api/projects/file/")
					.path(f.getProjectId().toString()).toUriString();
			logger.info("@ProjectFileService.insertFileRecordsAsTemp publicUrl: "+publicUrl);
			jdbc.update(sql, f.getId(), f.getProjectId(), f.getTempPath().toString(), publicUrl,
					f.getOriginalFileName());
		}
	}

	/**
	 * Register after-commit action: move temp files to final folder and update DB
	 * file_path to final path. This will be invoked AFTER transaction successfully
	 * commits.
	 */
	public void registerAfterCommitMoveAndUpdate(List<FileTemp> temps) {
		if (temps == null || temps.isEmpty())
			return;

		// ensure a transaction is active
		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			ProjectFileService.this.moveAndUpdateDirect(temps);
			return;
		}

		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				try {
					// Use outer class reference
					ProjectFileService.this.moveAndUpdateDirect(temps);
				} catch (Exception e) {
					logger.error("Failed moving files after commit", e);
				}
			}
		});
	}

	/**
	 * Move files and update DB accordingly. Runs after commit (separate DB calls
	 * allowed).
	 */
	private void moveAndUpdateDirect(List<FileTemp> temps) {
		String updateSql = "UPDATE projects_files SET file_path = ? WHERE project_file_id = ?";
		for (FileTemp f : temps) {
			try {
				// ensure final folder exists (created earlier but safe)
				Files.createDirectories(f.getFinalPath().getParent());
				Files.move(f.getTempPath(), f.getFinalPath(), StandardCopyOption.REPLACE_EXISTING);
				// update DB to final path
				jdbc.update(updateSql, f.getFinalPath().toString(), f.getId());
			} catch (IOException ex) {
				logger.error("Failed moving temp file {} to final {}", f.getTempPath(), f.getFinalPath(), ex);
				// do NOT attempt to rollback — DB already committed. Best effort only.
			}
		}
	}

	/**
	 * Cleanup temp files on failure (called from facade catch block).
	 */
	public void cleanupTempFiles(List<FileTemp> temps) {
		if (temps == null)
			return;
		
		temps.forEach(t -> {
			try {
				Files.deleteIfExists(t.getTempPath());
			} catch (IOException ignored) {
			}
		});
	}
	
	public void cleanupTempFolder(UUID projectId) {
	    Path tempFolder = getTempFolder(projectId);
	    try {
	        if (Files.exists(tempFolder)) {
	            // Check if folder is empty
	            try (var files = Files.list(tempFolder)) {
	                if (files.findAny().isEmpty()) {
	                    Files.deleteIfExists(tempFolder);
	                    logger.info("@ProjectFileService.cleanupTempFolder Deleted empty temp folder: " + tempFolder);
	                }
	            }
	        }
	    } catch (Exception e) {
	        logger.error("Failed to delete temp folder " + tempFolder, e);
	    }
	}

}
