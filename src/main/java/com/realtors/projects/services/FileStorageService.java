package com.realtors.projects.services;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.realtors.common.config.FileStorageProperties;
import com.realtors.projects.dto.ProjectFileDto;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class FileStorageService {

	private static final Logger logger = LoggerFactory.getLogger(FileStorageService.class);
	private final FileStorageProperties fileStorageProperties;

	public FileStorageService(FileStorageProperties fileStorageProperties) {
		this.fileStorageProperties = fileStorageProperties;
	}
	
	@Value("${file.upload-dir}")
	private String uploadDir;
	
	/*
	 * public String saveProjectFile(UUID projectId, MultipartFile file) throws
	 * IOException { String baseFolder = fileStorageProperties.getUploadDir() +
	 * "/projects/" ; String projectFolder = baseFolder + projectId;
	 * Files.createDirectories(Paths.get(projectFolder)); String fileName =
	 * System.currentTimeMillis() + "_" + file.getOriginalFilename(); Path filePath
	 * = Paths.get(projectFolder, fileName); Files.write(filePath, file.getBytes());
	 * return filePath.toString(); }
	 */	
	public ProjectFileDto storeFile(UUID projectId, MultipartFile file) throws IOException {
		UUID fileId = UUID.randomUUID();
        Path projectFolder = fileStorageProperties.getProjectFolder(projectId, "projects");
        String uniqueName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
        Path target = projectFolder.resolve(uniqueName);

        // Save file to file system
        try {
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("File upload failed", e);
        }
        
        String publicUrl = ServletUriComponentsBuilder
                .fromCurrentContextPath()
                .path("/api/projects/file/")
                .path(fileId.toString())
                .toUriString();

        ProjectFileDto dto = new ProjectFileDto();
                dto.setProjectFileId(fileId);
                dto.setProjectId(projectId);
                dto.setFileName(file.getOriginalFilename());
                dto.setPublicUrl(publicUrl);
                dto.setFilePath(target.toString());
                dto.setSizeByts(0);
		return dto;
	}
	
	public boolean deleteFile(String filePath) {
		/*
		 * String baseFolder = fileStorageProperties.getUploadDir() + "/projects/" ;
		 * logger.info("@FileStorage.deletFile  baseFolder: "+ baseFolder); String
		 * fileFolder = baseFolder+projectId ;
		 * logger.info("@FileStorage.deletFile  baseFolder: "+ fileFolder); Path path =
		 * Paths.get(URI.create(fileFolder));
		 */
		boolean flag = false;
		String normalizedPath = filePath.replace("\\", "/");
		Path path = Paths.get(normalizedPath);
		logger.info("@FileStorage.deletFile path: "+path);
		try {
			flag = Files.deleteIfExists(path);
		} catch (IOException ioe) {
			logger.error("@FileStorage.deletFile Error when deleting the file: "+ ioe.getMessage());
		}
		logger.info("@FileStorage.deletFile flag: "+flag);
		return flag;
	}
}
