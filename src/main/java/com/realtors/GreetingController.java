package com.realtors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.realtors.common.ApiResponse;
import com.realtors.projects.controller.ProjectController;
import com.realtors.projects.dto.PlotUnitDto;
import com.realtors.projects.dto.ProjectDetailDto;
import com.realtors.projects.dto.ProjectSummaryDto;
import com.realtors.projects.services.PlotUnitService;
import com.realtors.projects.services.ProjectService;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/public")
public class GreetingController {
	
	private static final Logger logger = LoggerFactory.getLogger(ProjectController.class);
	private final ProjectService service;
	private final PlotUnitService plotService;
	
	public GreetingController(ProjectService service, PlotUnitService plotService) {
		this.service= service;	
		this.plotService = plotService;
	}

    @GetMapping
    public Map<String, String> getGreeting() {
        return Map.of("message", "This is Diamond Reality Services Application");
    }
    
    @GetMapping("/projects")
	public ResponseEntity<ApiResponse<List<ProjectSummaryDto>>> getAll() {
		List<ProjectSummaryDto> projects = service.getAciveProjects(); // active projects only
		return ResponseEntity.ok(ApiResponse.success("Projects Fetched", projects, HttpStatus.OK));
	}
    
    @GetMapping("/projects/details/{id}")
	public ResponseEntity<ApiResponse<ProjectDetailDto>> getProjectDetails(@PathVariable String id) {
		logger.info("@GreetingController.getProjectDetails id:  "+id);
		UUID projectId = UUID.fromString(id);
		ProjectDetailDto projects = service.getProjectDetails(projectId); // active projects only
		return ResponseEntity.ok(ApiResponse.success("Projects Fetched", projects, HttpStatus.OK));
	}
    
    @GetMapping("/plots/{id}")
	public ResponseEntity<ApiResponse<PlotUnitDto>> getPlotData(@PathVariable String id) {
		logger.info("@GreetingController.getProjectDetails id:  "+id);
		UUID plotId = UUID.fromString(id);
		PlotUnitDto plot = plotService.getByPlotId(plotId); // active projects only
		return ResponseEntity.ok(ApiResponse.success("Projects Fetched", plot, HttpStatus.OK));
	}
}
