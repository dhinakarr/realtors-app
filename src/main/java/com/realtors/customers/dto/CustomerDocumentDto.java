package com.realtors.customers.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerDocumentDto {
    private long documentId;
    private UUID customerId;
    private String documentNumber;
    private String documentType;
    private String fileName;
    private String filePath;
    private UUID uploadedBy;
    private LocalDateTime uploadedAt;
}