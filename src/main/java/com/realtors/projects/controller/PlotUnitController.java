package com.realtors.projects.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.realtors.common.ApiResponse;
import com.realtors.common.Utils;
import com.realtors.projects.dto.PlotUnitDto;
import com.realtors.projects.services.PlotUnitService;

import java.util.*;

@RestController
@RequestMapping("/api/plots")
public class PlotUnitController {

    private final PlotUnitService service;
    
    public PlotUnitController(PlotUnitService plotService) {
    	this.service = plotService;
    }

    // AUTO GENERATE PLOTS AFTER PROJECT CREATION
    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<PlotUnitDto>> generate(@RequestParam UUID projectId,
                                      @RequestParam int total,
                                      @RequestParam int startNumber) {

    	service.generatePlots(projectId, total, startNumber);
        return ResponseEntity.ok(ApiResponse.success( "Plots generated successfully", null, HttpStatus.OK));
    }

    @GetMapping("/project/{projectId}")
    public ResponseEntity<ApiResponse<List<PlotUnitDto>>> listByProject(@PathVariable UUID projectId) {
    	List<PlotUnitDto> list = service.getByProject(projectId);
        return ResponseEntity.ok(ApiResponse.success("Plot Units are fetched", list));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PlotUnitDto>> create(@RequestBody PlotUnitDto dto) {
    	PlotUnitDto data = service.createPlot(dto);
        return ResponseEntity.ok(ApiResponse.success("Plot Units are fetched", data));
    }

    @PatchMapping
    public ResponseEntity<?> update(@RequestBody PlotUnitDto dto) {
        return ResponseEntity.ok(
                Map.of("updated", service.update(dto))
        );
    }
    
    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<PlotUnitDto>> update(@PathVariable UUID id,
            @RequestBody Map<String, Object> otherFields) {
    	Set<String> integerFields = Set.of("area", "basePrice", "width", "breath", "totalPrice"); 
    	Map<String, Object> updates = Utils.castNumberFields(otherFields, integerFields);
    	PlotUnitDto updated = service.patchUpdate(id, updates);
        return ResponseEntity.ok(ApiResponse.success("Plot Units are fetched", updated) );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Plot Units are fetched", null));
    }
}
