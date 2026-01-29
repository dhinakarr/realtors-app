package com.realtors.common.service;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.logging.Logger;

import org.springframework.stereotype.Component;

import com.realtors.common.config.FileStorageProperties;

@Component
public class FileSavingService {

	protected final Logger logger = Logger.getLogger(getClass().getName());
	private final FileStorageProperties fileStorageProperties;

	public FileSavingService(FileStorageProperties fileStorageProperties) {
		this.fileStorageProperties = fileStorageProperties;
	}

	private Path getFolderPath(UUID id, String module, String folder) {
		return Paths.get(fileStorageProperties.getUploadDir(), module, id.toString(), folder);
	}

	private String getPublicPath(UUID id, String module, String folderName) {
		return String.format("/files/%s/%s/%s/", module, id, folderName.replace("/", ""));
	}

	public String saveFile(FileStorageContext context) {
		if (context.profileImage() == null || context.profileImage().isEmpty()) {
			return null;
		}

		String publicUrl = getPublicPath(context.uniqueId(), context.moduleName(), context.lastFolder());
		Path folderPath = getFolderPath(context.uniqueId(), context.moduleName(), context.lastFolder());

		try {
			Files.createDirectories(folderPath);
			// 🔥 delete existing files
			try (DirectoryStream<Path> stream = Files.newDirectoryStream(folderPath)) {
				for (Path path : stream) {
					Files.deleteIfExists(path);
				}
			}

			Path filePath = folderPath.resolve(context.profileImage().getOriginalFilename());
			context.profileImage().transferTo(filePath.toFile());
			return publicUrl + context.profileImage().getOriginalFilename();
		} catch (IOException e) {
			logger.severe("File upload failed: " + e.getMessage());
			throw new RuntimeException("Failed to save file", e);
		}
	}
	
	public void deleteDocument(UUID userId, String moduleName, String lastFolder, String fileName) {
	    try {
	        Path folderPath = getFolderPath(userId, moduleName, lastFolder);
	        Path fullPath = folderPath.resolve(fileName);

	        Files.deleteIfExists(fullPath);
	    } catch (IOException e) {
	        throw new RuntimeException("Failed to delete document file", e);
	    }
	}


}
