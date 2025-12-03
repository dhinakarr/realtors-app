package com.realtors.customers.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.realtors.admin.dto.form.DynamicFormResponseDto;
import com.realtors.admin.dto.form.EditResponseDto;
import com.realtors.admin.service.AbstractBaseService;
import com.realtors.common.config.FileStorageProperties;
import com.realtors.common.util.AppUtil;
import com.realtors.customers.dto.CustomerDocumentDto;
import com.realtors.customers.dto.CustomerDto;
import com.realtors.customers.repository.CustomerDocumentRepository;
import com.realtors.customers.repository.CustomerRepository;

@Service
public class CustomerService extends AbstractBaseService<CustomerDto, UUID> {
	
	protected final Logger logger = Logger.getLogger(getClass().getName());
	private String uploadDir;
	private CustomerRepository customerRepo;
	private CustomerDocumentRepository documentRepo;
	private final FileStorageProperties fileStorageProperties;
	private final JdbcTemplate jdbc;

	public CustomerService(CustomerRepository customerRepo, CustomerDocumentRepository documentRepo,
			FileStorageProperties fileStorageProperties, JdbcTemplate jdbc) {
		super(CustomerDto.class, "customers", jdbc);
		this.customerRepo = customerRepo;
		this.documentRepo = documentRepo;
		this.fileStorageProperties = fileStorageProperties;
		this.jdbc = jdbc;
	}

	@Override
	protected String getIdColumn() {
		return "customer_id";
	}

	public DynamicFormResponseDto getCustomerForm() {
		return super.buildDynamicFormResponse();
	}

	public EditResponseDto<CustomerDto> getEditForm(UUID customerId) {
		CustomerDto dto = getCustomer(customerId);
		DynamicFormResponseDto form = super.buildDynamicFormResponse();
		return new EditResponseDto<>(dto, form);
	}

	public List<CustomerDto> getAllCustomers() {
		return customerRepo.findAllWithDocuments();
	}
	
	public CustomerDto createCustomer(CustomerDto dto, MultipartFile profileImage) throws Exception {
		UUID id = UUID.randomUUID();
		dto.setCustomerId(id);
		logger.info("CustomerService.createCustomer request received");
		String uploadDir = fileStorageProperties.getUploadDir();
		// Profile Image upload
		if (profileImage != null && !profileImage.isEmpty()) {
			String folder = uploadDir + "/customers/" + id + "/profile/";
			new File(folder).mkdirs();

			String filePath = folder + profileImage.getOriginalFilename();
			profileImage.transferTo(new File(filePath));
			dto.setProfileImagePath(filePath);
		}
		logger.info("CustomerService.createCustomer file storage completed");
		UUID userId = AppUtil.getCurrentUserId();
		dto.setStatus("ACTIVE");
		dto.setCreatedBy(userId);
		// dto.setUpdatedBy(userId);
		customerRepo.save(dto);
		logger.info("CustomerService.createCustomer data saved");
		return getCustomer(id);
	}

	public void uploadDocument(UUID customerId, String docType, MultipartFile file, UUID uploadedBy) throws Exception {
		String folder = uploadDir + "/customers/" + customerId + "/documents/";
		new File(folder).mkdirs();

		String filePath = folder + file.getOriginalFilename();
		file.transferTo(new File(filePath));

		CustomerDocumentDto doc = new CustomerDocumentDto();
		doc.setCustomerId(customerId);
		doc.setDocumentType(docType);
		doc.setFileName(file.getOriginalFilename());
		doc.setFilePath(filePath);
		doc.setUploadedBy(uploadedBy);

		documentRepo.save(doc);
	}

	public CustomerDto getCustomer(UUID id) {
		CustomerDto dto = customerRepo.findById(id);
		dto.setDocuments(documentRepo.findByCustomer(id));
		return dto;
	}

	public CustomerDto updateCustomer(CustomerDto dto) {
		customerRepo.updateCustomer(dto);
		return getCustomer(dto.getCustomerId());
	}

	public boolean deleteCustomer(UUID customerId) {
		return super.softDelete(customerId);
	}

	public void deleteDocument(UUID docId) {
		String path = documentRepo.findFilePath(docId);
		// delete file from server
		if (path != null) {
			File file = new File(path);
			if (file.exists())
				file.delete();
		}
		// delete DB record
		documentRepo.delete(docId);
	}

	public void updateCustomerWithProfileImage(CustomerDto dto, MultipartFile profileImage) {
		UUID customerId = dto.getCustomerId();
		if (customerId == null) {
			throw new IllegalArgumentException("customerId is required for update");
		}

		// If profile image is provided, store it and set new profileImageUrl on DTO
		if (profileImage != null && !profileImage.isEmpty()) {
			// get existing image path (so we can delete it)
			String existingPath = customerRepo.findProfileImagePath(customerId);

			// create folder and save new image
			String folder = Paths.get(uploadDir, "customers", customerId.toString(), "profile").toString();
			File dir = new File(folder);
			if (!dir.exists())
				dir.mkdirs();

			String filename = System.currentTimeMillis() + "_" + profileImage.getOriginalFilename();
			Path target = Paths.get(folder, filename);
			try {
				profileImage.transferTo(target.toFile());
			} catch (IOException e) {
				throw new RuntimeException("Failed to save profile image", e);
			}

			String newUrl = target.toString();
			dto.setProfileImagePath(newUrl);

			// delete old file (best-effort)
			if (existingPath != null && !existingPath.isBlank()) {
				try {
					Files.deleteIfExists(Paths.get(existingPath));
				} catch (IOException ignored) {
					/* ignore */ }
			}
		}

		// Perform the partial update (fields that are null will not override existing
		// ones)
		customerRepo.updatePartial(dto);
	}

	// Document download helper
	public CustomerDocumentDto getDocumentById(UUID docId) {
		return documentRepo.findById(docId);
	}
}
