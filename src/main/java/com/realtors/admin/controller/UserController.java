package com.realtors.admin.controller;

import com.realtors.admin.dto.AppUserDto;
import com.realtors.common.ApiResponse;
import com.realtors.common.util.AppUtil;
import com.realtors.admin.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);
    
    @Autowired
    private UserService appUserService;
    
    public UserController(UserService appUserService) {
    	this.appUserService = appUserService;
    }

    /** ✅ Create user */
    @PostMapping
    public ResponseEntity<ApiResponse<AppUserDto>> createUser(@RequestBody AppUserDto dto) {
    	
        if (dto.getEmail() == null || dto.getEmail().isBlank()) {
        	logger.debug("Email should not be Empty");
            return ResponseEntity.badRequest().body(ApiResponse.failure("Email is required", HttpStatus.BAD_REQUEST));
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
                .orElseGet(() -> ResponseEntity.badRequest().body(ApiResponse.failure("User not found", HttpStatus.NOT_FOUND)));
    }

    /** ✅ Update user */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<AppUserDto>> updateUser(@PathVariable UUID id,
                                                              @RequestBody AppUserDto dto) {
        AppUserDto updated =  appUserService.update(id, dto);
        
        if (updated == null) {
            return ResponseEntity
                    .badRequest()
                    .body(ApiResponse.failure("User not found", HttpStatus.NOT_FOUND));
        }

        return ResponseEntity.ok(
                ApiResponse.success("User updated successfully", updated, HttpStatus.OK)
        );
    }
    
    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<AppUserDto>> patch(
            @PathVariable UUID id,
            @RequestBody Map<String, Object> updates) {
        AppUserDto updated = appUserService.partialUpdate(id, updates);
        return ResponseEntity.ok(ApiResponse.success("Updated successfully", updated, HttpStatus.OK));
    }

    /** ✅ Soft delete */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> softDelete(@PathVariable UUID id) {
        boolean deleted = appUserService.softDelete(id);
        return deleted
                ?  ResponseEntity.ok(ApiResponse.success("User marked as INACTIVE", null, HttpStatus.OK))
                :  ResponseEntity.badRequest().body(ApiResponse.failure("User not found", HttpStatus.NOT_FOUND));
    }

    /** ✅ Update meta (JSONB) */
    @PatchMapping("/{id}/meta")
    public ResponseEntity<ApiResponse<AppUserDto>> updateMeta(@PathVariable UUID id,
                                                        @RequestBody Map<String, Object> meta) {
    	AppUserDto updated = appUserService.patch(id, meta);
    	if (updated == null) {
            return ResponseEntity
                    .badRequest()
                    .body(ApiResponse.failure("User not found", HttpStatus.NOT_FOUND));
        }

        return ResponseEntity.ok(
                ApiResponse.success("User updated successfully", updated, HttpStatus.OK)
        );
    }

    /** ✅ Update last login */
    @PatchMapping("/{id}/last-login")
    public ResponseEntity<ApiResponse<Void>> updateLastLogin(@PathVariable UUID id) {
        UUID loggedInUser = AppUtil.getCurrentUserId();
        if (loggedInUser == null) {
            return  ResponseEntity.badRequest().body(ApiResponse.failure("Unauthorized", HttpStatus.UNAUTHORIZED));
        }

        boolean updated = appUserService.updateLastLogin(id);
        return updated
                ?  ResponseEntity.ok(ApiResponse.success("Last login updated", null, HttpStatus.OK))
                :  ResponseEntity.badRequest().body(ApiResponse.failure("User not found", HttpStatus.NOT_FOUND));
    }
}

