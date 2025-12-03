package com.realtors.customers.repository;

import java.util.List;
import java.util.UUID;

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
				    (document_id, customer_id, document_type, file_name, file_path, uploaded_by)
				    VALUES (?, ?, ?, ?, ?, ?)
				""";
		jdbc.update(sql, d.getDocumentId(), d.getCustomerId(), d.getDocumentType(), d.getFileName(), d.getFilePath(),
				d.getUploadedBy());
	}

	public List<CustomerDocumentDto> findByCustomer(UUID customerId) {
		String sql = "SELECT * FROM customer_documents WHERE customer_id = ? ORDER BY uploaded_at DESC";

		return jdbc.query(sql, new Object[] { customerId }, (rs, rowNum) -> {
			CustomerDocumentDto d = new CustomerDocumentDto();
			d.setDocumentId(rs.getLong("document_id"));
			d.setCustomerId(UUID.fromString(rs.getString("customer_id")));
			d.setDocumentType(rs.getString("document_type"));
			d.setFileName(rs.getString("file_name"));
			d.setFilePath(rs.getString("file_path"));
			d.setUploadedBy(rs.getString("uploaded_by") != null ? UUID.fromString(rs.getString("uploaded_by")) : null);
			d.setUploadedAt(rs.getTimestamp("uploaded_at").toLocalDateTime());
			return d;
		});
	}

	public CustomerDocumentDto findById(UUID docId) {
		return jdbc.queryForObject("SELECT *  FROM customer_documents WHERE document_id = ?", new Object[] { docId },
				CustomerDocumentDto.class);
	}

	public String findFilePath(UUID docId) {
		return jdbc.queryForObject("SELECT file_path FROM customer_documents WHERE document_id = ?", new Object[] { docId },
				String.class);
	}

	public void delete(UUID docId) {
		jdbc.update("DELETE FROM customer_documents WHERE document_id = ?", docId);
	}
}
