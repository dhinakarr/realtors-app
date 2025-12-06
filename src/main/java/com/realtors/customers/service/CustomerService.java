package com.realtors.customers.service;

import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
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
import com.realtors.common.util.AppUtil;
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
		List<CustomerDto> list = super.findAll();
		for(CustomerDto dto: list) {
			
		}
		return customerRepo.findAllWithDocuments();
	}
	
	private String getFolderPath(UUID id, String folder) {
		String uploadDir = fileStorageProperties.getUploadDir();
		String retString = uploadDir + "/customers/" + id + folder;
		return retString;
	}
	
	private String getPublicPath(UUID id, String folderName) {
		return "/files/customers/" + id + folderName;
	}
	
	private String saveFile(MultipartFile profileImage, UUID customerId, String lastFolder) {
		String publicUrl = getPublicPath(customerId, lastFolder);
		if (profileImage != null && !profileImage.isEmpty()) {
			String folder = getFolderPath(customerId, lastFolder);  //uploadDir + "/customers/" + id + "/profile/";
			new File(folder).mkdirs();

			String filePath = folder + profileImage.getOriginalFilename();
			try {
				profileImage.transferTo(new File(filePath));
			} catch(IOException ioe) {
				logger.severe("CustomerService.saveFile error in saving file: "+ioe);
			}
			return (new StringBuilder().append(publicUrl).append(profileImage.getOriginalFilename()).toString());
		}
		return null;
	}
	
	public List<CustomerDto> search(String searchText) {
		return super.search(searchText, List.of("customer_name", "email", "address", "occupation"), null);
	}
	
	@Transactional(value="txManager")
	public CustomerDto createCustomer(CustomerDto dto, MultipartFile profileImage) throws Exception {
		CustomerDto created = super.create(dto);
		UUID customerId = created.getCustomerId(); 
		String imagePathUrl = saveFile(profileImage, customerId, "/profile/");
	
		customerRepo.updatePartial(customerId, Map.of("profile_image_path", imagePathUrl));
		created.setProfileImagePath(imagePathUrl);
		return created;
	}
	
	@Transactional("txManager")
	public void uploadDocument(UUID customerId, String documentType, String documentNumber, MultipartFile file) throws Exception {

		if (file == null ) {
			throw new IllegalArgumentException("No files uploaded");
		}
		String imagePathUrl = saveFile(file, customerId, "/documents/");
	
		CustomerDocumentDto docDto = new CustomerDocumentDto();
		docDto.setCustomerId(customerId);
		docDto.setDocumentNumber(documentNumber);
		docDto.setDocumentType(documentType);
		docDto.setFileName(file.getOriginalFilename());
		docDto.setFilePath(imagePathUrl);
		docDto.setUploadedAt(LocalDateTime.now());
		docDto.setUploadedBy(AppUtil.getCurrentUserId());
		documentRepo.save(docDto);
	}

	
	public List<CustomerDocumentDto> getAllDocuments(UUID customerId) {
		return documentRepo.findByCustomer(customerId);
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
			dbImagePathDecoded = URLDecoder.decode(dbImagePath, StandardCharsets.UTF_8.toString());
		} catch (Exception e) {
            throw new IOException("@CustomerService.deleteImage Path decoding failed: ", e);
        }
		final String PREFIX_TO_REMOVE = "/files/customers/"+customerId+"/profile/";
		final String fileName = dbImagePathDecoded.substring(PREFIX_TO_REMOVE.length());
		Path fileToDelete = Paths.get(getFolderPath(customerId, "/profile/"), fileName);
		try {
            if (Files.exists(fileToDelete)) {
                Files.delete(fileToDelete);
            } else {
            	logger.severe("@CustomerService.deleteImage File not found on disk after decoding: " + fileToDelete.toString());
            }
        } catch (IOException e) {
            throw new IOException("@CustomerService.deleteImage Failed to delete the profile image file: " + fileToDelete.toString(), e);
        }
		fileToDelete = null;
		customerRepo.updatePartial(customerId, Map.of("profile_image_path", ""));
		return true;
	}

	public CustomerDto getCustomer(UUID id) {
		CustomerDto dto = super.findById(id).stream().findFirst().get(); //findById(id);
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

	public void deleteDocument(Long docId) {
		CustomerDocumentDto dto = documentRepo.findById(docId);
		UUID customerId = dto.getCustomerId();
		String filePath = getFolderPath(customerId, "/documents/");
		logger.info("@CustomerService.deleteDocument filePath: "+ filePath);
		String fullPath = filePath+dto.getFileName();
		logger.info("@CustomerService.deleteDocument fullPath with file name: "+ fullPath);
		if (fullPath != null) {
			File file = new File(fullPath);
			if (file.exists())
				file.delete();
		}
		// delete DB record
		documentRepo.delete(docId);
	}

	public CustomerDto updateCustomerWithProfileImage(UUID customerId, Map<String, Object> updates, MultipartFile profileImage) {
		String publicUrl = saveFile(profileImage, customerId, "/profile/");
		return customerRepo.updatePartial(customerId, Map.of("profile_image_path", publicUrl));
	}

	// Document download helper
	public CustomerDocumentDto getDocumentById(Long docId) {
		return documentRepo.findById(docId);
	}
}
