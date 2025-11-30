package com.realtors.admin.controller;

import com.realtors.admin.dto.ModuleDto;
import com.realtors.admin.dto.PagedResult;
import com.realtors.admin.dto.form.DynamicFormResponseDto;
import com.realtors.admin.dto.form.EditResponseDto;
import com.realtors.common.ApiResponse;
import com.realtors.admin.service.ModuleService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/modules")
public class ModuleController {

    private static final Logger logger = LoggerFactory.getLogger(ModuleController.class);
    private final ModuleService moduleService;

    public ModuleController(ModuleService moduleService) {
        this.moduleService = moduleService;
    }

    @GetMapping("/form")
	public ResponseEntity<ApiResponse<DynamicFormResponseDto>> getUserForm() {
		DynamicFormResponseDto roles = moduleService.getModulesFormData();
		return ResponseEntity.ok(ApiResponse.success("Users fetched successfully", roles, HttpStatus.OK));
	}
    
    @GetMapping("/editForm/{id}")
	public ResponseEntity<ApiResponse<EditResponseDto<ModuleDto>>> getUserEditForm(@PathVariable UUID id) {
		EditResponseDto<ModuleDto> users = moduleService.editModulesResponse(id);
		return ResponseEntity.ok(ApiResponse.success("Users fetched successfully", users, HttpStatus.OK));
	}
    
    // ✅ Create
    @PostMapping
    public ResponseEntity<ApiResponse<ModuleDto>> createModule(
            @Valid @RequestBody ModuleDto dto) {

        ModuleDto created = moduleService.createModule(dto);
        logger.info("Module created successfully: "+created.getModuleName());
        return ResponseEntity.ok(ApiResponse.success("Module created successfully", created, HttpStatus.OK));
    }

    // ✅ Get All
    @GetMapping
    public ResponseEntity<ApiResponse<List<ModuleDto>>> getAllModules() {
        List<ModuleDto> modules = moduleService.getAllModules();
        return ResponseEntity.ok(ApiResponse.success("Modules fetched successfully", modules, HttpStatus.OK));
    }
    
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<ModuleDto>>> searchModules(String searchText) {
        List<ModuleDto> modules =  moduleService.searchModules(searchText);
        return ResponseEntity.ok(ApiResponse.success("Active modules fetched", modules));
    }
    
    @GetMapping("/pages")
    public ResponseEntity<ApiResponse<PagedResult<ModuleDto>>> getPagedData(int page, int size) {
    	PagedResult<ModuleDto> modules =  moduleService.getPaginatedModules(page,size);
        return ResponseEntity.ok(ApiResponse.success("Active modules fetched", modules));
    }

    // ✅ Get by ID
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ModuleDto>> getModuleById(@PathVariable UUID id) {
        Optional<ModuleDto> module = moduleService.getModuleById(id);
        return module.map(m -> ResponseEntity.ok(ApiResponse.success("Module fetched", m, HttpStatus.OK)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.failure("Module not found", HttpStatus.NOT_FOUND)));
    }

    // ✅ Update
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ModuleDto>> updateModule(
            @PathVariable UUID id,
            @Valid @RequestBody ModuleDto dto) {

        ModuleDto updated = moduleService.updateModule(id, dto);
        return ResponseEntity.ok(ApiResponse.success("Module updated successfully", updated, HttpStatus.OK));
    }
    
    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<ModuleDto>> patch(
            @PathVariable UUID id,
            @RequestBody Map<String, Object> updates) {
    	ModuleDto updated = moduleService.partialUpdate(id, updates);
        return ResponseEntity.ok(ApiResponse.success("Updated successfully", updated, HttpStatus.OK));
    }

    // ✅ Soft Delete
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteModule(
            @PathVariable UUID id) {
        moduleService.deleteModule(id);
        return ResponseEntity.ok(ApiResponse.success("Module deleted successfully", null, HttpStatus.OK));
    }
}
