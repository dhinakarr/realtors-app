package com.realtors.admin.controller;

import com.realtors.admin.dto.PagedResult;
import com.realtors.admin.dto.RoleDto;
import com.realtors.admin.dto.RoleHierarchyDto;
import com.realtors.admin.dto.form.DynamicFormResponseDto;
import com.realtors.admin.dto.form.EditResponseDto;
import com.realtors.admin.service.RoleService;
import com.realtors.common.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/roles")
public class RoleController {

    private final RoleService rolesService;
    private static final Logger logger = LoggerFactory.getLogger(RoleController.class);

    public RoleController(RoleService rolesService) {
        this.rolesService = rolesService;
    }
    
    @GetMapping("/form")
	public ResponseEntity<ApiResponse<DynamicFormResponseDto>> getUserForm() {
		DynamicFormResponseDto roles = rolesService.getRolesFormData();
		return ResponseEntity.ok(ApiResponse.success("Users fetched successfully", roles, HttpStatus.OK));
	}
    
    @GetMapping("/editForm/{id}")
	public ResponseEntity<ApiResponse<EditResponseDto<RoleDto>>> getRoleEditForm(@PathVariable UUID id) {
		EditResponseDto<RoleDto> roles = rolesService.editRolesResponse(id);
		return ResponseEntity.ok(ApiResponse.success("Users fetched successfully", roles, HttpStatus.OK));
	}

    @GetMapping
    public ResponseEntity<ApiResponse<List<RoleDto>>> listAllRoles() {
        List<RoleDto> roles =  rolesService.getAllRoles();
        return ResponseEntity.ok(ApiResponse.success("Active roles fetched", roles));
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Optional<RoleDto>>> listByRoleId(@PathVariable UUID id ) {
        Optional<RoleDto> roles =  rolesService.getRoleById(id);
        return ResponseEntity.ok(ApiResponse.success("Active roles fetched", roles));
    }
    
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<RoleDto>>> listSearchData(String searchText) {
        List<RoleDto> roles =  rolesService.searchRoles(searchText);
        return ResponseEntity.ok(ApiResponse.success("Active roles fetched", roles));
    }
    
    @GetMapping("/pages")
    public ResponseEntity<ApiResponse<PagedResult<RoleDto>>> getPagedData(int page, int size) {
    	PagedResult<RoleDto> roles =  rolesService.getPaginatedData(page,size);
        return ResponseEntity.ok(ApiResponse.success("Active roles fetched", roles));
    }
    
    // ✅ Create role
    @PostMapping
    public ResponseEntity<ApiResponse<RoleDto>> createRole(@RequestBody RoleDto role) {
    	RoleDto dto = rolesService.createRole(role);
    	return ResponseEntity.ok(ApiResponse.success("Role created successfully", dto, HttpStatus.OK));
        
    }

    // ✅ Update role
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<RoleDto>> updateRole(
            @PathVariable UUID id,
            @RequestBody RoleDto role) {
    	RoleDto dto = rolesService.updateRoleById(id, role);
    	return ResponseEntity.ok(ApiResponse.success("Role created successfully", dto, HttpStatus.OK));
    }
    
    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<RoleDto>> patchRole(
            @PathVariable UUID id,
            @RequestBody Map<String, Object> dto) {

    	RoleDto updated = rolesService.patchRoleUpdate(id, dto);
        return ResponseEntity.ok(ApiResponse.success("Role updated successfully", updated, HttpStatus.OK));
    }

    // ✅ Soft delete
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> softDeleteRole(@PathVariable UUID id) {
        try {
            rolesService.deleteRole(id);
            return ResponseEntity.ok(ApiResponse.success("Role deactivated successfully", null, HttpStatus.OK));
        } catch (Exception ex) {
            logger.error("Failed to soft delete role", ex);
            return ResponseEntity.internalServerError().body(ApiResponse.failure("Failed to deactivate role"));
        }
    }
    
    @GetMapping("/{id}/subroles")
    public ResponseEntity<ApiResponse<List<RoleHierarchyDto>>> getSubRoles(@PathVariable UUID id) {
        List<RoleHierarchyDto> result = rolesService.findSubRoles(id);
        return ResponseEntity.ok(ApiResponse.success("Sub roles fetched successfully", result, HttpStatus.OK));
    }

    @GetMapping("/{id}/parents")
    public ResponseEntity<ApiResponse<List<RoleHierarchyDto>>> getParentChain(@PathVariable UUID id) {
        List<RoleHierarchyDto> result = rolesService.findParentChain(id);
        return ResponseEntity.ok(ApiResponse.success("Sub roles fetched successfully", result, HttpStatus.OK));
    }
}
