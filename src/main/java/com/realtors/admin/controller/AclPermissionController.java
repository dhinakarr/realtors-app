package com.realtors.admin.controller;


import com.realtors.admin.dto.AclPermissionDto;
import com.realtors.common.ApiResponse;
import com.realtors.admin.service.AclPermissionService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/permissions")
public class AclPermissionController {

    private final AclPermissionService permissionService;

    public AclPermissionController(AclPermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<AclPermissionDto>>> getAll() {
    	List<AclPermissionDto> list = this.permissionService.findAll();
        return ResponseEntity.ok(com.realtors.common.ApiResponse.success("Acl Data Fetched", list,  HttpStatus.OK));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Optional<AclPermissionDto>>> getById(@PathVariable UUID id) {
    	Optional<AclPermissionDto> dto = permissionService.findById(id);
        return ResponseEntity.ok(ApiResponse.success("Acl Data Fetched", dto, HttpStatus.OK));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AclPermissionDto>> create(@RequestBody AclPermissionDto dto) {
    	AclPermissionDto acl = permissionService.create(dto);
        return ResponseEntity.ok(ApiResponse.success("Acl Data Created", acl, HttpStatus.OK));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<AclPermissionDto>> update(@PathVariable UUID id, @RequestBody AclPermissionDto dto) {
    	AclPermissionDto data = permissionService.update(id, dto);
        return ResponseEntity.ok(ApiResponse.success("Acl data updated", data, HttpStatus.OK));
    }
    
    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<AclPermissionDto>> partialUpdate(@PathVariable UUID id, @RequestBody Map<String, Object> dto) {
    	AclPermissionDto data = permissionService.partialUpdate(id, dto);
        return ResponseEntity.ok(ApiResponse.success("Acl data updated", data, HttpStatus.OK));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
    	permissionService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Acl data deleted", null, HttpStatus.OK));
    }
}

