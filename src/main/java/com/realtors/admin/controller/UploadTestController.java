package com.realtors.admin.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.realtors.admin.dto.UploadTestDto;
import com.realtors.admin.service.UploadTestService;

import java.util.UUID;

@RestController
@RequestMapping("/test")
public class UploadTestController {

	@Autowired
	private UploadTestService service;

	// Create user + upload profile image
	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<?> registerUser(@RequestPart("user") UploadTestDto userDto,
			@RequestPart(value = "profileImage", required = false) MultipartFile profileImage) {
		try {
			UUID userId = service.createUser(userDto, profileImage);
			return ResponseEntity.ok().body("{\"userId\": \"" + userId + "\"}");
		} catch (Exception e) {
			return ResponseEntity.badRequest().body("Error: " + e.getMessage());
		}
	}

	// Get user profile + image
	@GetMapping("/{userId}")
	public ResponseEntity<?> getUser(@PathVariable UUID userId) throws Exception {
		UploadTestDto user = service.findById(userId);
		if (user == null) {
			return ResponseEntity.notFound().build();
		}
		return ResponseEntity.ok(user);
	}

	// Get only profile image
	@GetMapping(value = "/{userId}/image", produces = { "image/jpeg", "image/png", "image/gif" })
	public ResponseEntity<byte[]> getProfileImage(@PathVariable UUID userId) throws Exception {
		UploadTestDto user = service.findById(userId);
		if (user == null || user.getProfileImage() == null) {
			return ResponseEntity.notFound().build();
		}

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.IMAGE_JPEG); // You can detect actual type if needed
		headers.setContentLength(user.getProfileImage().length);
		return new ResponseEntity<>(user.getProfileImage(), headers, HttpStatus.OK);
	}

	// Update profile image
	@PatchMapping(value = "/{userId}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<String> updateImage(@PathVariable UUID userId, @RequestPart("image") MultipartFile image,
			@RequestPart(value = "meta", required = false) String metaPatch) {
		try {
			boolean updated = service.updateProfileImage(userId, image, metaPatch);
			return updated ? ResponseEntity.ok("Profile image updated") : ResponseEntity.notFound().build();
		} catch (Exception e) {
			return ResponseEntity.badRequest().body("Error: " + e.getMessage());
		}
	}

}
