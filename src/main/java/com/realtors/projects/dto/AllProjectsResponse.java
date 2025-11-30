package com.realtors.projects.dto;

import java.util.List;

public record AllProjectsResponse(List<ProjectDto> dto, List<ProjectFileDto> file) {}
