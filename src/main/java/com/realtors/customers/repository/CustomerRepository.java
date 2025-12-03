package com.realtors.customers.repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.realtors.common.util.AppUtil;
import com.realtors.customers.dto.CustomerDocumentDto;
import com.realtors.customers.dto.CustomerDto;

@Repository
public class CustomerRepository {

	private JdbcTemplate jdbc;
	protected final Logger logger = Logger.getLogger(getClass().getName());
	public CustomerRepository(JdbcTemplate jdbc) {
		this.jdbc = jdbc;
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
			return dto;
		});
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
			String sql = "SELECT profile_image_path FROM customer_details WHERE customer_id = ?";
			return jdbc.queryForObject(sql, new Object[] { customerId }, String.class);
		} catch (EmptyResultDataAccessException ex) {
			return null;
		}
	}

	public int updatePartial(CustomerDto dto) {
		String sql = """
				    UPDATE customers
				    SET
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
				        profile_image_path = COALESCE(?, profile_image_pathl),
				        notes = COALESCE(?, notes),
				        status = COALESCE(?, status),
				        updated_at = NOW(),
				        updated_by = COALESCE(?, updated_by)
				    WHERE customer_id = ?
				""";

		return jdbc.update(sql, dto.getCustomerName(), dto.getEmail(), dto.getMobile(), dto.getDataOfBirth(),
				dto.getGender(), dto.getAddress(), dto.getCity(), dto.getState(), dto.getPincode(), dto.getAltMobile(),
				dto.getOccupation(), dto.getProfileImagePath(), dto.getNotes(), dto.getStatus(), AppUtil.getCurrentUserId(),
				dto.getCustomerId() // WHERE customer_id = ?
		);
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
	                customer.setProfileImagePath(rs.getString("profile_image_path"));
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
