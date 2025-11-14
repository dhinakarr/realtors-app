package com.realtors.admin.controller;

import com.realtors.admin.dto.AppUserDto;
import com.realtors.common.ApiResponse;
import com.realtors.admin.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/appUsers")
@Validated
public class AppUserController {

    private static final Logger logger = LoggerFactory.getLogger(AppUserController.class);

    @Autowired
    private UserService appUserService;

    /** ✅ Create user */
    @PostMapping
    public ResponseEntity<ApiResponse<AppUserDto>> createUser(@RequestBody AppUserDto dto) {
        if (dto.getEmail() == null || dto.getEmail().isBlank()) {
        	logger.debug("Email should not be null");
        	return ResponseEntity.badRequest()
                    .body(ApiResponse.failure("User email is required", HttpStatus.BAD_REQUEST));
        }
        AppUserDto created = appUserService.createUser(dto);
        return ResponseEntity.ok(ApiResponse.success("User created successfully", created, HttpStatus.CREATED));
    }

    /** ✅ Get all users */
    @GetMapping
    public ResponseEntity<ApiResponse<List<AppUserDto>>> getAllUsers() {
        List<AppUserDto> users = appUserService.getAllUsers();
        return ResponseEntity.ok(ApiResponse.success("Users fetched successfully", users, HttpStatus.OK));
    }

    /** ✅ Get user by ID */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AppUserDto>> getUserById(@PathVariable UUID id) {
        Optional<AppUserDto> user = appUserService.getUserById(id);
        return user.map(u -> ResponseEntity.ok(ApiResponse.success("User found", u, HttpStatus.OK)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.failure("User not found", HttpStatus.NOT_FOUND)));
    }

    /** ✅ Update user */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<AppUserDto>> updateUser(@PathVariable UUID id, @RequestBody AppUserDto dto) {
        dto.setUserId(id);
        AppUserDto updated = appUserService.updateUser(dto, id);
        if (updated == null) {
        	return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ApiResponse.failure("User not found", HttpStatus.NOT_FOUND));
        }
        return  ResponseEntity.ok(ApiResponse.success("User updated successfully", updated, HttpStatus.OK));
    }

    /** ✅ Soft delete */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> softDelete(@PathVariable UUID id, @RequestParam UUID updatedBy) {
        boolean deleted = appUserService.softDeleteUser(id);
        return deleted
                ? ResponseEntity.ok(ApiResponse.success("User marked as INACTIVE", null, HttpStatus.OK))
                : ResponseEntity.badRequest().body(ApiResponse.failure("User not found", HttpStatus.NOT_FOUND));
    }

    /** ✅ Update meta (JSONB) */
    @PatchMapping("/{id}/meta")
    public ResponseEntity<ApiResponse<Void>> updateMeta(@PathVariable UUID id, @RequestBody Map<String, Object> meta) {
        boolean updated = appUserService.updateMeta(id, meta);
        return updated
                ? ResponseEntity.ok(ApiResponse.success("Meta updated successfully", null, HttpStatus.OK))
                : ResponseEntity.badRequest().body(ApiResponse.failure("Failed to update meta", HttpStatus.BAD_REQUEST));
    }

    /** ✅ Update last login */
    @PatchMapping("/{id}/last-login")
    public ResponseEntity<ApiResponse<Void>> updateLastLogin(@PathVariable UUID id) {
        boolean updated = appUserService.updateLastLogin(id);
        return updated
                ? ResponseEntity.ok(ApiResponse.success("Last login updated", null, HttpStatus.OK))
                : ResponseEntity.badRequest().body(ApiResponse.failure("User not found", HttpStatus.NOT_FOUND));
    }
}
