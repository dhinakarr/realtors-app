package com.realtors.projects.dto;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProjectFileDto {
	private UUID projectFileId;
	private UUID projectId;
	private String filePath;
	private String fileName;
	private String publicUrl;
	private int sizeByts;

}
