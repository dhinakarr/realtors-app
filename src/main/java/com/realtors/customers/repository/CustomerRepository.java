package com.realtors.customers.repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import com.realtors.common.config.FileStorageProperties;
import com.realtors.common.util.AppUtil;
import com.realtors.customers.dto.CustomerDocumentDto;
import com.realtors.customers.dto.CustomerDto;

@Repository
public class CustomerRepository {

	private JdbcTemplate jdbc;
	private final FileStorageProperties props;
	protected final Logger logger = Logger.getLogger(getClass().getName());
	public CustomerRepository(JdbcTemplate jdbc, FileStorageProperties props) {
		this.jdbc = jdbc;
		this.props = props;
	}

	public void save(CustomerDto dto) {
		String sql = """
				    INSERT INTO customers
				    (customer_name, email, mobile, date_of_birth, gender,
				     address, city, state, pincode, alt_mobile, occupation,
				     profile_image_path, notes, status, created_by)
				    VALUES ( ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) 
				""";
		logger.info("CustomerRepository.save Data received CustomerName: "+ dto.getCustomerName());
		jdbc.update(sql, dto.getCustomerName(), dto.getEmail(), dto.getMobile(),
				dto.getDataOfBirth(), dto.getGender(), dto.getAddress(), dto.getCity(), dto.getState(),
				dto.getPincode(), dto.getAltMobile(), dto.getOccupation(), dto.getProfileImagePath(), dto.getNotes(),
				dto.getStatus(), AppUtil.getCurrentUserId() // created_by (placeholder)
		);
		logger.info("CustomerRepository.save Data received Customer data saved: ");
	}

	public CustomerDto findById(UUID id) {
		String sql = "SELECT * FROM customers WHERE customer_id = ?";

		return jdbc.queryForObject(sql, new Object[] { id }, (rs, rowNum) -> {
			CustomerDto dto = new CustomerDto();
			dto.setCustomerId(UUID.fromString(rs.getString("customer_id")));
			dto.setCustomerName(rs.getString("customer_name"));
			dto.setEmail(rs.getString("email"));
			dto.setMobile(rs.getLong("mobile"));
			dto.setDataOfBirth(rs.getDate("date_of_birth"));
			dto.setGender(rs.getString("gender"));
			dto.setAddress(rs.getString("address"));
			dto.setCity(rs.getString("city"));
			dto.setState(rs.getString("state"));
			dto.setPincode(rs.getString("pincode"));
			dto.setAltMobile(rs.getLong("alt_mobile"));
			dto.setOccupation(rs.getString("occupation"));
			dto.setProfileImagePath(rs.getString("profile_image_path"));
			dto.setNotes(rs.getString("notes"));
			dto.setStatus(rs.getString("status"));
			logger.info("@CustomerRepository.findById publicURL: "+dto.getProfileImagePath());
			return dto;
		});
	}

	/*
	 * private String convertToPublicUrl(String fullPath) { if (fullPath == null)
	 * return null; // Normalize windows path String normalized =
	 * fullPath.replace("\\", "/").replace(" ", "%20"); int idx =
	 * normalized.indexOf("/uploads/"); if (idx != -1) { return "/files" +
	 * normalized.substring(idx + "/uploads".length()); } return null; }
	 */

	private String convertToPublicUrl(String fullPath) {
	    if (fullPath == null) return null;

	    String root = props.getUploadDir().replace("\\", "/");
	    String normalized = fullPath.replace("\\", "/").replace(" ", "%20");

	    if (normalized.startsWith(root)) {
	        String relative = normalized.substring(root.length());
	        if (!relative.startsWith("/")) relative = "/" + relative;
	        return "/files" + relative;
	    }

	    return null;
	}
	
	public void updateCustomer(CustomerDto dto) {

		String sql = """
				    UPDATE customers SET
				        customer_name = COALESCE(?, customer_name),
				        email = COALESCE(?, email),
				        mobile = COALESCE(?, mobile),
				        date_of_birth = COALESCE(?, date_of_birth),
				        gender = COALESCE(?, gender),
				        address = COALESCE(?, address),
				        city = COALESCE(?, city),
				        state = COALESCE(?, state),
				        pincode = COALESCE(?, pincode),
				        alt_mobile = COALESCE(?, alt_mobile),
				        occupation = COALESCE(?, occupation),
				        profile_image_path = COALESCE(?, profile_image_path),
				        notes = COALESCE(?, notes),
				        status = COALESCE(?, status),
				        updated_by = COALESCE(?, updated_by),
				        updated_at = NOW()
				    WHERE customer_id = ?
				""";

		jdbc.update(sql, dto.getCustomerName(), dto.getEmail(), dto.getMobile(), dto.getDataOfBirth(), dto.getGender(),
				dto.getAddress(), dto.getCity(), dto.getState(), dto.getPincode(), dto.getAltMobile(),
				dto.getOccupation(), dto.getProfileImagePath(), dto.getNotes(), dto.getStatus(),
				AppUtil.getCurrentUserId(), AppUtil.getCurrentUserId());
	}

	public String findProfileImagePath(UUID customerId) {
		try {
			String sql = "SELECT profile_image_path FROM customers WHERE customer_id = ?";
			return jdbc.queryForObject(sql, new Object[] { customerId }, String.class);
		} catch (EmptyResultDataAccessException ex) {
			return null;
		}
	}

	private String camelToSnake(String field) {
	    return field.replaceAll("([a-z])([A-Z]+)", "$1_$2").toLowerCase();
	}

	
	public CustomerDto updatePartial(UUID customerId, Map<String, Object> updates) {

	    if (updates == null || updates.isEmpty()) {
	        return findById(customerId); // nothing to update
	    }

	    Set<String> disallowed = Set.of("customerId", "createdBy", "createdAt");
	    StringBuilder sql = new StringBuilder("UPDATE customers SET ");
	    List<Object> params = new ArrayList<>();

	    updates.forEach((key, value) -> {
	        if (disallowed.contains(key)) {
	            return;
	        }
	        String column = camelToSnake(key);
	        sql.append(column).append(" = ?, ");
	        params.add(value);
	    });

	    // Remove trailing comma + space
	    if (sql.toString().endsWith(", ")) {
	        sql.setLength(sql.length() - 2);
	        sql.append(", "); // optional: ensure spacing before updated_at
	    }

	    // common fields
	    sql.append("updated_at = NOW(), updated_by = ? ");
	    params.add(AppUtil.getCurrentUserId());

	    // WHERE clause
	    sql.append("WHERE customer_id = ?");
	    params.add(customerId);

	    jdbc.update(sql.toString(), params.toArray());
	    return findById(customerId);
	}



	public List<CustomerDto> findAllWithDocuments() {
	    String sql = """
	       SELECT
	            c.customer_id, c.customer_name, c.email, c.mobile, c.date_of_birth, c.gender, c.address, c.city, c.state, c.pincode, c.alt_mobile, c.occupation,
	            c.profile_image_path, c.status, c.notes, 
	            d.document_id, d.document_type, d.document_number, d.file_path, d.file_name
	       FROM customers c
	       LEFT JOIN customer_documents d
	            ON c.customer_id = d.customer_id
	       ORDER BY c.customer_id
	    """;

	    return jdbc.query(sql, rs -> {
	        Map<UUID, CustomerDto> customerMap = new HashMap<>();

	        while (rs.next()) {
	            UUID cid = (UUID) rs.getObject("customer_id");

	            CustomerDto customer = customerMap.get(cid);
	            if (customer == null) {
	                customer = new CustomerDto();
	                customer.setCustomerId(cid);
	                customer.setCustomerName(rs.getString("customer_name"));
	                customer.setEmail(rs.getString("email"));
	                customer.setMobile(rs.getLong("mobile"));
	                customer.setDataOfBirth(rs.getDate("date_of_birth"));
	                customer.setGender(rs.getString("gender"));
	                customer.setOccupation(rs.getString("occupation"));
	                customer.setAddress(rs.getString("address"));
	                customer.setCity(rs.getString("city"));
	                customer.setState(rs.getString("state"));
	                customer.setPincode(rs.getString("pincode"));
	                customer.setAltMobile(rs.getLong("alt_mobile"));
	                customer.setNotes(rs.getString("notes"));
	                customer.setStatus(rs.getString("status"));
	                customer.setProfileImagePath(convertToPublicUrl(rs.getString("profile_image_path")));
	                customerMap.put(cid, customer);
	            }

	            Long docId = rs.getLong("document_id");
	            if (docId != 0) {
	                CustomerDocumentDto doc = new CustomerDocumentDto();
	                doc.setDocumentId(docId);
	                doc.setDocumentNumber(rs.getString("document_number"));
	                doc.setDocumentType(rs.getString("document_type"));
	                doc.setFilePath(rs.getString("file_path"));
	                doc.setFileName(rs.getString("file_name"));
	                customer.getDocuments().add(doc);
	            }
	        }

	        return new ArrayList<>(customerMap.values());
	    });
	}

}
