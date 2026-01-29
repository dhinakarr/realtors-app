package com.realtors.admin.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class UserDocumentDto {
	private Long documentId;
    private UUID userId;
    private String documentNumber;
    private String documentType;
    private String fileName;
    private String filePath;
    private UUID uploadedBy;
    private LocalDateTime uploadedAt;

}
