package com.realtors.projects.dto;

import java.nio.file.Path;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FileTemp {
    private UUID id;
    private UUID projectId;
    private String originalFileName;
    private Path tempPath;   // full absolute path to temp file
    private Path finalPath;  // computed final path under /projects/{projectId}/filename
    // getters/setters/constructors
}
