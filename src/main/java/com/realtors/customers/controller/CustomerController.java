package com.realtors.customers.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.realtors.admin.dto.form.DynamicFormResponseDto;
import com.realtors.admin.dto.form.EditResponseDto;
import com.realtors.common.ApiResponse;
import com.realtors.customers.dto.CustomerDocumentDto;
import com.realtors.customers.dto.CustomerDto;
import com.realtors.customers.service.CustomerService;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

	private CustomerService service;
	protected final Logger logger = Logger.getLogger(getClass().getName());
	public CustomerController(CustomerService service) {
		this.service = service;
	}
	
	@GetMapping("/form")
	public ResponseEntity<ApiResponse<DynamicFormResponseDto>> getCustomerForm() {
		DynamicFormResponseDto dto = service.getCustomerForm();
		return ResponseEntity.ok(ApiResponse.success("Customer Form Fields", dto, HttpStatus.OK));
	}
	
	@GetMapping("/form/{id}")
	public ResponseEntity<ApiResponse<EditResponseDto<CustomerDto>>> getEditForm(@PathVariable UUID id) {
		return ResponseEntity.ok(ApiResponse.success("Customer Form Fields",service.getEditForm(id)));
	}
	
	@GetMapping
	public ResponseEntity<ApiResponse<List<CustomerDto>>> getAiiCustomers() {
		return ResponseEntity.ok(ApiResponse.success("Customer Form Fields",service.getAllCustomers()));
	}

	@PostMapping(consumes = { "multipart/form-data" })	
	public ResponseEntity<ApiResponse<CustomerDto>> createCustomer(@RequestPart("customer") CustomerDto dto,
			@RequestPart(value = "profileImage", required = false) MultipartFile profileImage) throws Exception {
		
		CustomerDto created = service.createCustomer(dto, profileImage);
		return ResponseEntity.ok(ApiResponse.success("Customer Created", created, HttpStatus.CREATED));
	}

	@PostMapping(value="/{customerId}/documents", consumes = { "multipart/form-data" })
	public ResponseEntity<ApiResponse<?>> uploadDocument(@PathVariable UUID customerId, 
																								@RequestParam String documentType, @RequestParam String documentNumber,
																								@RequestParam MultipartFile files) throws Exception {
		service.uploadDocument(customerId, documentType, documentNumber, files);
		return ResponseEntity.ok(ApiResponse.success("Document uploaded", null));
	}

	@GetMapping("/{customerId}")
	public ResponseEntity<ApiResponse<CustomerDto>> getCustomer(@PathVariable UUID customerId) {
		CustomerDto dto = service.getCustomer(customerId);
//		logger.info("@CustomerController.getCustomer publicUrl: "+dto.getProfileImagePath());
		return ResponseEntity.ok(ApiResponse.success("Customer Data", dto));
	}
	
	@PatchMapping("/{customerId}")
	public ResponseEntity<ApiResponse<CustomerDto>> updateCustomer(@PathVariable UUID customerId,
	        @RequestBody CustomerDto dto
	) {
	    dto.setCustomerId(customerId);
	    CustomerDto data = service.updateCustomer(dto);
	    return ResponseEntity.ok(ApiResponse.success("Customer updated successfully", data));
	}
	
	@PatchMapping(value = "/{customerId}", consumes = { "multipart/form-data" })
    public ResponseEntity<ApiResponse<CustomerDto>> patchCustomer(@PathVariable UUID customerId,
            @RequestPart(value = "customer", required = false) Map<String, Object> otherFields,
            @RequestPart(value = "profileImage", required = false) MultipartFile profileImage ) {
		
		Map<String, Object> updates = new HashMap<String, Object>();
		
		if (profileImage != null) {
//			logger.info("@CustomerController.patchCustomer image section profileImage: "+profileImage);
			updates.put("profileImage", profileImage);
		}
		if (otherFields != null && !otherFields.isEmpty()) {
			updates.putAll(otherFields);
		}
		if (updates.isEmpty()) {
			CustomerDto data = service.getCustomer(customerId);
			return ResponseEntity.ok(ApiResponse.success("No update happened", data, HttpStatus.OK));
		}
        CustomerDto updated = service.updateCustomerWithProfileImage(customerId, updates, profileImage);
        return ResponseEntity.ok(ApiResponse.success("Customer updated successfully",  updated));
    }
	
	@DeleteMapping("/{customerId}")
	public ResponseEntity<ApiResponse<?>> deleteCustomer(@PathVariable UUID customerId) {
	    service.deleteCustomer(customerId);
	    return ResponseEntity.ok(ApiResponse.success("Customer marked as INACTIVE", null));
	}
	
	@DeleteMapping(value = "/documents/{docId}", produces = "application/json")
	public ResponseEntity<ApiResponse<?>> deleteDocument(@PathVariable Long docId) {
	    service.deleteDocument(docId);
	    return ResponseEntity.ok(ApiResponse.success("Document deleted", null));
	}
	
	@DeleteMapping("/image/{costomerId}")
	public ResponseEntity<ApiResponse<?>> deleteImage(@PathVariable UUID costomerId) {
//		logger.info("@CustomeController.deleteImage request received: " + costomerId);
	    try {
	    	service.deleteImage(costomerId);
	    } catch(IOException ioe) {
	    	ResponseEntity.internalServerError().body(ApiResponse.failure("Unable to delete file", HttpStatus.EXPECTATION_FAILED));
	    }
//	    logger.info("@CustomeController.deleteImage image deleted for: " + costomerId);
	    return ResponseEntity.ok(ApiResponse.success("Image deleted", HttpStatus.OK));
	}
	
	// Document download endpoint
		@GetMapping("/documents/{docId}/download")
		public ResponseEntity<Resource> downloadDocument(@PathVariable Long docId) throws IOException {
			CustomerDocumentDto doc = service.getDocumentById(docId);
			if (doc == null) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
			}

			Path path = Paths.get(doc.getFilePath());
			if (!Files.exists(path)) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
			}
			Resource resource = new UrlResource(path.toUri());
			String contentType = Files.probeContentType(path);
			if (contentType == null) {
				contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
			}

			String disposition = "attachment; filename=\"" + doc.getFileName() + "\"";
			return ResponseEntity.ok().contentType(MediaType.parseMediaType(contentType))
					.header(HttpHeaders.CONTENT_DISPOSITION, disposition).body(resource);
		}
		
		@GetMapping("/search")
		public ResponseEntity<ApiResponse<List<CustomerDto>>> searchCustomer(@RequestParam String searchText) {
			List<CustomerDto> list = service.search(searchText);
			return ResponseEntity.ok(ApiResponse.success("Search Data Fetched", list, HttpStatus.OK));
		}
		
		@GetMapping("/{customerId}/documents")
		public ResponseEntity<ApiResponse<List<CustomerDocumentDto>>> getDocuments(@PathVariable String customerId) {
			UUID custId = null;
			if (customerId != null) 
				custId = UUID.fromString(customerId);
			List<CustomerDocumentDto> list = service.getAllDocuments(custId);
			return ResponseEntity.ok(ApiResponse.success("Documents Data Fetched", list, HttpStatus.OK));
		}
}
