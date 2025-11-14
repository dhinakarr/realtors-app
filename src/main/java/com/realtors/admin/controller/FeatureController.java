package com.realtors.admin.controller;

import com.realtors.admin.dto.FeatureDto;
import com.realtors.admin.service.FeatureService;
import com.realtors.common.ApiResponse;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/features")
public class FeatureController {

    @Autowired
    private FeatureService featureService;

    @PostMapping
    public ResponseEntity<ApiResponse<FeatureDto>> createFeature(@RequestBody FeatureDto dto) {
        FeatureDto feature =  featureService.createFeature(dto);
        return ResponseEntity.ok(ApiResponse.success("Feature creaated successfully", feature, HttpStatus.OK));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<FeatureDto>>> getAllFeatures() {
    	List<FeatureDto> lst = featureService.findAll();
        return ResponseEntity.ok(ApiResponse.success("Feature creaated successfully", lst, HttpStatus.OK));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<FeatureDto>> getFeatureById(@PathVariable("id") UUID featureId) {
    	Optional<FeatureDto> feature = featureService.getFeatureById(featureId);
    	return feature.map(f -> ResponseEntity.ok(ApiResponse.success("Feature fetched", f, HttpStatus.OK)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.failure("Feature not found", HttpStatus.NOT_FOUND)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<FeatureDto>> updateFeature(
            @PathVariable("id") UUID featureId,
            @RequestBody FeatureDto dto
    ) {
        FeatureDto updated = featureService.updateFeature(featureId, dto);
        return ResponseEntity.ok(ApiResponse.success("Feature updated successfully", updated, HttpStatus.OK));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<FeatureDto>> patch(
            @PathVariable UUID id,
            @RequestBody Map<String, Object> updates) {
        FeatureDto updated = featureService.partialUpdate(id, updates);
        return ResponseEntity.ok(ApiResponse.success("Updated successfully", updated, HttpStatus.OK));
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteFeature(@PathVariable("id") UUID featureId) {
        featureService.deleteFeature(featureId);
        return ResponseEntity.ok(ApiResponse.success("Feature deleted successfully", null, HttpStatus.OK));
    }
}
