package com.realtors.admin.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.realtors.admin.dto.AppUserDto;
import com.realtors.admin.dto.PagedResult;
import com.realtors.admin.dto.UserMiniDto;
import com.realtors.admin.dto.UserTreeDto;
import com.realtors.admin.dto.form.DynamicFormResponseDto;
import com.realtors.admin.dto.form.EditResponseDto;
import com.realtors.common.ApiResponse;
import com.realtors.common.util.AppUtil;
import com.realtors.admin.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
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

	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<ApiResponse<AppUserDto>> createUser(
	        @RequestPart("dto") String dtoJson,
	        @RequestPart(value = "profileImage", required = false) MultipartFile profileImage
	) {
	    try {
	        ObjectMapper mapper = new ObjectMapper();
	        AppUserDto dto = mapper.readValue(dtoJson, AppUserDto.class);

	        if (dto.getEmail() == null || dto.getMobile() == null) {
	            return ResponseEntity.badRequest()
	                    .body(ApiResponse.failure("Email and Mobile are required", HttpStatus.BAD_REQUEST));
	        }

	        AppUserDto created = appUserService.createWithFiles(dto, profileImage);
	        return ResponseEntity.ok(
	                ApiResponse.success("Users Created successfully", created, HttpStatus.OK)
	        );

	    } catch (Exception ex) {
	        ex.printStackTrace();
	        return ResponseEntity.badRequest()
	                .body(ApiResponse.failure(ex.getMessage(), HttpStatus.BAD_REQUEST));
	    }
	}


	@GetMapping("/form")
	public ResponseEntity<ApiResponse<DynamicFormResponseDto>> getUserForm() {
		DynamicFormResponseDto users = appUserService.getUserFormData();
		return ResponseEntity.ok(ApiResponse.success("Users fetched successfully", users, HttpStatus.OK));
	}

	@GetMapping("/editForm/{id}")
	public ResponseEntity<ApiResponse<EditResponseDto<AppUserDto>>> getUserEditForm(@PathVariable UUID id) {
		EditResponseDto<AppUserDto> users = appUserService.editUserResponse(id);
		return ResponseEntity.ok(ApiResponse.success("Users fetched successfully", users, HttpStatus.OK));
	}
	
	@GetMapping("/role/{id}")
	public ResponseEntity<ApiResponse<List<UserMiniDto>>> getUsersByRole(@PathVariable UUID id) {
		List<UserMiniDto> users = appUserService.getUsersByRole(id);
		return ResponseEntity.ok(ApiResponse.success("Users fetched successfully", users, HttpStatus.OK));
	}

	/** ✅ Get all users */
	@GetMapping
	public ResponseEntity<ApiResponse<List<AppUserDto>>> getAllUsers() {
		List<AppUserDto> users = appUserService.getAllUsers();
		return ResponseEntity.ok(ApiResponse.success("Users fetched successfully", users, HttpStatus.OK));
	}
	
	@GetMapping("/tree")
	public ResponseEntity<ApiResponse<List<UserTreeDto>>> getUserTree() {
		List<UserTreeDto> users = appUserService.findUserTree();
		return ResponseEntity.ok(ApiResponse.success("Users fetched successfully", users, HttpStatus.OK));
	}

	@GetMapping("/search")
	public ResponseEntity<ApiResponse<List<AppUserDto>>> searchModules(@RequestParam String searchText) {
		List<AppUserDto> users = appUserService.searchUsers(searchText);
		return ResponseEntity.ok(ApiResponse.success("Active Users fetched", users));
	}

	@GetMapping("/pages")
	public ResponseEntity<ApiResponse<PagedResult<AppUserDto>>> getPagedData(@RequestParam int page,
			@RequestParam int size) {
		PagedResult<AppUserDto> users = appUserService.getPaginatedUsers(page, size);
		return ResponseEntity.ok(ApiResponse.success("Active Users fetched", users));
	}

	/** ✅ Get user by ID */
	@GetMapping("/{id}")
	public ResponseEntity<ApiResponse<AppUserDto>> getUserById(@PathVariable String id) {
		try {
	        UUID uuid = UUID.fromString(id);
	        return appUserService.getUserById(uuid)
	            .map(u -> ResponseEntity.ok(ApiResponse.success("User found", u, HttpStatus.OK)))
	            .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
	                .body(ApiResponse.failure("User not found", HttpStatus.NOT_FOUND)));
	    } catch (IllegalArgumentException e) {
	        return ResponseEntity.badRequest()
	            .body(ApiResponse.failure("Invalid UUID format", HttpStatus.BAD_REQUEST));
	    }
	}

	/** ✅ Update user */
	@PutMapping("/{id}")
	public ResponseEntity<ApiResponse<AppUserDto>> updateUser(@PathVariable UUID id, @RequestBody AppUserDto dto) {
		AppUserDto updated = appUserService.update(id, dto);

		if (updated == null) {
			return ResponseEntity.badRequest().body(ApiResponse.failure("User not found", HttpStatus.NOT_FOUND));
		}

		return ResponseEntity.ok(ApiResponse.success("User updated successfully", updated, HttpStatus.OK));
	}

	@PatchMapping(path = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<ApiResponse<AppUserDto>> patch(@PathVariable UUID id,
			@RequestPart(value = "meta", required = false) String meta,
			@RequestPart(value = "profileImage", required = false) MultipartFile profileImage,
			@RequestParam Map<String, String> otherFields) { // This contains all other fields

		Map<String, Object> updates = new HashMap<String, Object>();

		// 1. Process meta (JSON string)
		if (meta != null && !meta.isBlank()) {
			try {
				// ObjectMapper works fine here
				Map<String, Object> metaMap = new ObjectMapper().readValue(meta,
						new TypeReference<Map<String, Object>>() {
						});
//				logger.info("@UserController.patch metaMap: "+metaMap.toString());
				updates.put("meta", metaMap);
			} catch (Exception e) {
				// Consider logging the error and returning a 400 Bad Request
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid JSON for meta field", e);
			}
		}

		// 2. Process profileImage (File)
		if (profileImage != null) {
			updates.put("profileImage", profileImage);
		}

		// 3. Process all other changed fields (from the frontend's FormData)
		// otherFields correctly contains ONLY the fields that were changed in the UI,
		// excluding meta and profileImage, because that's what the frontend sends.
		if (otherFields != null && !otherFields.isEmpty()) {
			updates.putAll(otherFields);
		}

		// Check if any updates were actually sent
		if (updates.isEmpty()) {
			Optional<AppUserDto> dto = appUserService.getUserById(id);
			// Return a response indicating nothing was updated (e.g., 304 Not Modified or a
			// success message with the current record)
			return dto.map(u -> ResponseEntity.ok(ApiResponse.success("User found", u, HttpStatus.OK)))
					.orElseGet(() -> ResponseEntity.badRequest()
							.body(ApiResponse.failure("User not found", HttpStatus.NOT_FOUND)));
		}

		// Call your service method with only the changed fields
		AppUserDto updated = appUserService.updateWithFiles(id, updates);
//		logger.info("@UserController.patch data updated: " + updated);
		return ResponseEntity.ok(ApiResponse.success("Updated successfully", updated, HttpStatus.OK));
	}

	/** ✅ Soft delete */
	@DeleteMapping("/{id}")
	public ResponseEntity<ApiResponse<Void>> softDelete(@PathVariable UUID id) {
		boolean deleted = appUserService.softDelete(id);
		return deleted ? ResponseEntity.ok(ApiResponse.success("User marked as INACTIVE", null, HttpStatus.OK))
				: ResponseEntity.badRequest().body(ApiResponse.failure("User not found", HttpStatus.NOT_FOUND));
	}

	/** ✅ Update meta (JSONB) */
	@PatchMapping("/{id}/meta")
	public ResponseEntity<ApiResponse<AppUserDto>> updateMeta(@PathVariable UUID id,
			@RequestBody Map<String, Object> meta) {
		AppUserDto updated = appUserService.patch(id, meta);
		if (updated == null) {
			return ResponseEntity.badRequest().body(ApiResponse.failure("User not found", HttpStatus.NOT_FOUND));
		}

		return ResponseEntity.ok(ApiResponse.success("User updated successfully", updated, HttpStatus.OK));
	}

	/** ✅ Update last login */
	@PatchMapping("/{id}/last-login")
	public ResponseEntity<ApiResponse<Void>> updateLastLogin(@PathVariable UUID id) {
		UUID loggedInUser = AppUtil.getCurrentUserId();
		if (loggedInUser == null) {
			return ResponseEntity.badRequest().body(ApiResponse.failure("Unauthorized", HttpStatus.UNAUTHORIZED));
		}

		boolean updated = appUserService.updateLastLogin(id);
		return updated ? ResponseEntity.ok(ApiResponse.success("Last login updated", null, HttpStatus.OK))
				: ResponseEntity.badRequest().body(ApiResponse.failure("User not found", HttpStatus.NOT_FOUND));
	}
}
