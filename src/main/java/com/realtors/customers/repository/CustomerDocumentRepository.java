package com.realtors.customers.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.realtors.customers.dto.CustomerDocumentDto;

@Repository
public class CustomerDocumentRepository {

	private JdbcTemplate jdbc;

	public CustomerDocumentRepository(JdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	public void save(CustomerDocumentDto d) {
		String sql = """
				    INSERT INTO customer_documents
				    (customer_id, document_type, document_number, file_name, file_path, uploaded_at, uploaded_by)
				    VALUES (?, ?, ?, ?, ?, ?, ?)
				""";
		jdbc.update(sql, d.getCustomerId(), d.getDocumentType(), d.getDocumentNumber(), d.getFileName(), d.getFilePath(),
				d.getUploadedAt(), d.getUploadedBy());
	}

	public List<CustomerDocumentDto> findByCustomer(UUID customerId) {
		String sql = "SELECT * FROM customer_documents WHERE customer_id = ? ORDER BY uploaded_at DESC";

		return jdbc.query(sql, new Object[] { customerId }, (rs, rowNum) -> {
			CustomerDocumentDto d = new CustomerDocumentDto();
			d.setDocumentId(rs.getLong("document_id"));
			d.setCustomerId(UUID.fromString(rs.getString("customer_id")));
			d.setDocumentType(rs.getString("document_type"));
			d.setDocumentType(rs.getString("document_number"));
			d.setFileName(rs.getString("file_name"));
			d.setFilePath(rs.getString("file_path"));
			d.setUploadedBy(rs.getString("uploaded_by") != null ? UUID.fromString(rs.getString("uploaded_by")) : null);
			d.setUploadedAt(rs.getTimestamp("uploaded_at").toLocalDateTime());
			return d;
		});
	}

	public CustomerDocumentDto findById(Long docId) {
		return jdbc.queryForObject("SELECT *  FROM customer_documents WHERE document_id = ?", new BeanPropertyRowMapper<>(CustomerDocumentDto.class),
		        docId);
	}

	public String findFilePath(UUID docId) {
		return jdbc.queryForObject("SELECT file_path FROM customer_documents WHERE document_id = ?", new Object[] { docId },
				String.class);
	}

	public void delete(Long docId) {
		jdbc.update("DELETE FROM customer_documents WHERE document_id = ?", docId);
	}
}
