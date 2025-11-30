package com.realtors.admin.controller;

import com.realtors.admin.dto.FeatureDto;
import com.realtors.admin.dto.PagedResult;
import com.realtors.admin.dto.form.DynamicFormResponseDto;
import com.realtors.admin.dto.form.EditResponseDto;
import com.realtors.admin.service.AclPermissionService;
import com.realtors.admin.service.FeatureService;
import com.realtors.common.ApiResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Optional;

@RestController
@RequestMapping("/api/features")
public class FeatureController {

    @Autowired
    private FeatureService featureService;
    private static final Logger log = LoggerFactory.getLogger(AclPermissionService.class);
    
    @GetMapping("/form")
	public ResponseEntity<ApiResponse<DynamicFormResponseDto>> getUserForm() {
		DynamicFormResponseDto roles = featureService.getRolesFormData();
		return ResponseEntity.ok(ApiResponse.success("Users fetched successfully", roles, HttpStatus.OK));
	}
    
    @GetMapping("/editForm/{id}")
	public ResponseEntity<ApiResponse<EditResponseDto<FeatureDto>>> getUserEditForm(@PathVariable UUID id) {
		EditResponseDto<FeatureDto> users = featureService.editRolesResponse(id);
		return ResponseEntity.ok(ApiResponse.success("Users fetched successfully", users, HttpStatus.OK));
	}

    @PostMapping
    public ResponseEntity<ApiResponse<FeatureDto>> createFeature(@RequestBody FeatureDto dto) {
    	log.info("FeatureController incoming dto: {}", dto.toString());
        FeatureDto feature =  featureService.createFeature(dto);
        return ResponseEntity.ok(ApiResponse.success("Feature creaated successfully", feature, HttpStatus.OK));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<FeatureDto>>> getAllFeatures() {
    	List<FeatureDto> lst = featureService.findAll();
        return ResponseEntity.ok(ApiResponse.success("Feature creaated successfully", lst, HttpStatus.OK));
    }
    
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<FeatureDto>>> listSearchData(String searchText) {
        List<FeatureDto> features =  featureService.searchFeatures(searchText);
        return ResponseEntity.ok(ApiResponse.success("Active features fetched", features));
    }
    
    @GetMapping("/pages")
    public ResponseEntity<ApiResponse<PagedResult<FeatureDto>>> getPagedData(@RequestParam int page, @RequestParam int size) {
    	PagedResult<FeatureDto> features =  featureService.getPaginated(page,size);
        return ResponseEntity.ok(ApiResponse.success("Active features fetched", features));
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
