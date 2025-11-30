package com.realtors.projects.dto;

import java.util.List;

public record ProjectResponse(ProjectDto dto, List<ProjectFileDto> file) {}
