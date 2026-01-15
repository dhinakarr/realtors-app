package com.realtors.projects.dto;

import java.util.List;

public record ProjectDetailDto(ProjectSummaryDto project, List<PlotUnitDto> plots, PlotUnitStatus stat) {}