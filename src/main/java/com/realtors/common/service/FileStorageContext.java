package com.realtors.common.service;

import java.util.UUID;

import org.springframework.web.multipart.MultipartFile;

public record FileStorageContext(
		MultipartFile profileImage, 
		UUID uniqueId, 
		String moduleName, 
		String lastFolder
		) {}
