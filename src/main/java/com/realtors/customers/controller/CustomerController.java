package com.realtors.customers.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
import com.realtors.common.util.AppUtil;
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
	
	@GetMapping("/form{/id}")
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
logger.info("CustomerController.createCustomer data received dto.getName(): "+dto.getCustomerName());
		CustomerDto created = service.createCustomer(dto, profileImage);
		logger.info("CustomerController.createCustomer data received data saved: "+created.getCustomerName());
		return ResponseEntity.ok(ApiResponse.success("Customer Created", created, HttpStatus.CREATED));
	}

	@PostMapping("/{customerId}/documents")
	public ResponseEntity<ApiResponse<?>> uploadDocument(@PathVariable UUID customerId, @RequestParam String docType,
			@RequestParam MultipartFile file) throws Exception {
		UUID uploadedBy = AppUtil.getCurrentUserId();
		service.uploadDocument(customerId, docType, file, uploadedBy);
		return ResponseEntity.ok(ApiResponse.success("Document uploaded", null));
	}

	@GetMapping("/{customerId}")
	public ResponseEntity<ApiResponse<CustomerDto>> getCustomer(@PathVariable UUID customerId) {
		return ResponseEntity.ok(ApiResponse.success("Customer Data", service.getCustomer(customerId)));
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
    public ResponseEntity<?> patchCustomer(@PathVariable UUID customerId,
            @RequestPart(value = "customer", required = true) CustomerDto dto,
            @RequestPart(value = "profileImage", required = false) MultipartFile profileImage
    ) {
        dto.setCustomerId(customerId);
        service.updateCustomerWithProfileImage(dto, profileImage);
        return ResponseEntity.ok(Map.of("message", "Customer updated"));
    }
	
	@DeleteMapping("/{customerId}")
	public ResponseEntity<ApiResponse<?>> deleteCustomer(@PathVariable UUID customerId) {
	    service.deleteCustomer(customerId);
	    return ResponseEntity.ok(ApiResponse.success("Customer marked as INACTIVE", null));
	}
	
	@DeleteMapping("/documents/{docId}")
	public ResponseEntity<ApiResponse<?>> deleteDocument(@PathVariable UUID docId) {
	    service.deleteDocument(docId);
	    return ResponseEntity.ok(ApiResponse.success("Document deleted", null));
	}
	
	// Document download endpoint
		@GetMapping("/documents/{docId}/download")
		public ResponseEntity<Resource> downloadDocument(@PathVariable UUID docId) throws IOException {
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
}
