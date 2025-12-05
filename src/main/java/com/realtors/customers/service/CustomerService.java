package com.realtors.customers.service;

import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.realtors.admin.dto.form.DynamicFormResponseDto;
import com.realtors.admin.dto.form.EditResponseDto;
import com.realtors.admin.service.AbstractBaseService;
import com.realtors.common.config.FileStorageProperties;
import com.realtors.customers.dto.CustomerDocumentDto;
import com.realtors.customers.dto.CustomerDto;
import com.realtors.customers.repository.CustomerDocumentRepository;
import com.realtors.customers.repository.CustomerRepository;

@Service
public class CustomerService extends AbstractBaseService<CustomerDto, UUID> {
	
	protected final Logger logger = Logger.getLogger(getClass().getName());
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
	
	private String getFolderPath(UUID id) {
		String uploadDir = fileStorageProperties.getUploadDir();
		String folder = uploadDir + "/customers/" + id + "/profile/";
		return folder;
	}
	
	private String getPublicPath(UUID id) {
		return "/files/customers/" + id + "/profile/";
	}
	
	@Transactional(value="txManager")
	public CustomerDto createCustomer(CustomerDto dto, MultipartFile profileImage) throws Exception {
		logger.info("CustomerService.createCustomer request received: "+dto.getDataOfBirth());
		CustomerDto created = super.create(dto);
		UUID customerId = created.getCustomerId(); 
		String publicUrl = getPublicPath(customerId);
		String imagePathUrl = null;
		// Profile Image upload
		if (profileImage != null && !profileImage.isEmpty()) {
			String folder = getFolderPath(customerId);  //uploadDir + "/customers/" + id + "/profile/";
			new File(folder).mkdirs();

			String filePath = folder + profileImage.getOriginalFilename();
			logger.info("CustomerService.createCustomer filePath: "+filePath);
			profileImage.transferTo(new File(filePath));
			imagePathUrl = (new StringBuilder().append(publicUrl).append(profileImage.getOriginalFilename()).toString());
		}
		logger.info("CustomerService.createCustomer file storage completed imagePathUrl: "+imagePathUrl);
		customerRepo.updatePartial(customerId, Map.of("profile_image_path", imagePathUrl));
		created.setProfileImagePath(imagePathUrl);
		logger.info("CustomerService.createCustomer publicUrl: "+created.getProfileImagePath());
		return created;
	}
	
	public void uploadDocument(UUID customerId, String docType, MultipartFile file, UUID uploadedBy) throws Exception {
		String folder = getFolderPath(customerId); //uploadDir + "/customers/" + customerId + "/documents/";
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
	
	@Transactional("txManager")
	public boolean deleteImage(UUID customerId) throws IOException{
		CustomerDto dto = getCustomer(customerId);
		String dbImagePath = dto.getProfileImagePath();
		
		if (dbImagePath == null || dbImagePath.isEmpty()) {
            return true;
        }
		String dbImagePathDecoded = null;
		try {
            // Use UTF-8 standard for decoding
			dbImagePathDecoded = URLDecoder.decode(dbImagePath, StandardCharsets.UTF_8.toString());
			logger.info("@CustomerService.deleteImage  dbImagePathDecoded:  " + dbImagePathDecoded);
		} catch (Exception e) {
            // Handle decoding failure (unlikely for %20)
            logger.severe("@CustomerService.deleteImage  Failed to decode URL path: " + dbImagePathDecoded);
            throw new IOException("@CustomerService.deleteImage Path decoding failed: ", e);
        }
		final String PREFIX_TO_REMOVE = "/files/customers/"+customerId+"/profile/";
		final String fileName = dbImagePathDecoded.substring(PREFIX_TO_REMOVE.length());
		/*
		 * String originalFileName = dbImagePathDecoded.startsWith("/") ?
		 * dbImagePathDecoded.substring(1) : dbImagePathDecoded;
		 */
		logger.info("@CustomerService.deleteImage  getFolderPath(customerId):  " + getFolderPath(customerId));
		logger.info("@CustomerService.deleteImage  originalFileName:  " + fileName);
		Path fileToDelete = Paths.get(getFolderPath(customerId), fileName);
		
		try {
            if (Files.exists(fileToDelete)) {
                Files.delete(fileToDelete);
            logger.info("@CustomerService.deleteImage Successfully deleted file: " + fileToDelete.toString());
            Thread.sleep(50);
            } else {
            	logger.severe("@CustomerService.deleteImage File not found on disk after decoding: " + fileToDelete.toString());
            }
        } catch (InterruptedException ex) { 
        	throw new RuntimeException("@CustomerService.deleteImage Failed to save profile image: ", ex); 
        } catch (IOException e) {
            throw new IOException("@CustomerService.deleteImage Failed to delete the profile image file: " + fileToDelete.toString(), e);
        }
		customerRepo.updatePartial(customerId, Map.of("profile_image_path", ""));
		return true;
	}

	public CustomerDto getCustomer(UUID id) {
		CustomerDto dto = super.findById(id).stream().findFirst().get(); //findById(id);
		logger.info("@CustomerService.getCustomer publicURL: "+dto.toString());
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

	public CustomerDto updateCustomerWithProfileImage(UUID customerId, Map<String, Object> updates, MultipartFile profileImage) {
	    if (profileImage != null && !profileImage.isEmpty()) {
	        String folder = getFolderPath(customerId);  //Paths.get(uploadDir, "customers", customerId.toString(), "profile").toString();
	        logger.info("@CustomerService.updateCustomerWithProfileImage folder: "+folder);
	        File dir = new File(folder);
	        if (!dir.exists()) dir.mkdirs();

	        String filename = profileImage.getOriginalFilename();
	        Path target = Paths.get(folder, filename);
	        logger.info("@CustomerService.updateCustomerWithProfileImage Path target: "+target.toString());
	        String existingPath = customerRepo.findProfileImagePath(customerId);
	        try { 
	        	profileImage.transferTo(target.toFile()); 
	        	if (existingPath != null)
		            Files.deleteIfExists(Paths.get(existingPath));
	        	Thread.sleep(50);
	        } catch (InterruptedException ex) { 
	        	throw new RuntimeException("@CustomerService.updateCustomerWithProfileImag Failed to save profile image: ", ex); 
	        } catch (IOException e) { 
	        	throw new RuntimeException("@CustomerService.updateCustomerWithProfileImag Failed to save profile image: ", e); 
	        }
	        // Add this field to updates map!!
	        updates.put("profile_image_path", getPublicPath(customerId)+filename);
	    }
	    // final DB update
	    return customerRepo.updatePartial(customerId, updates);
	}

	// Document download helper
	public CustomerDocumentDto getDocumentById(UUID docId) {
		return documentRepo.findById(docId);
	}
}
