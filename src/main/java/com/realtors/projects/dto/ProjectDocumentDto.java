package com.realtors.projects.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class ProjectDocumentDto {
	private Long documentId;
    private UUID projectId;
    private String documentNumber;
    private String documentType;
    private String fileName;
    private String filePath;
    private UUID uploadedBy;
    private LocalDateTime uploadedAt;

}
