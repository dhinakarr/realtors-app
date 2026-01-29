package com.realtors.sales.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.BeanPropertyRowMapper;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import com.realtors.dashboard.dto.ReceivableDetailDTO;
import com.realtors.dashboard.dto.SaleDetailDTO;
import com.realtors.sales.dto.SaleDTO;
import com.realtors.sales.dto.SalesStatus;
import com.realtors.sales.finance.dto.CashFlowItemDTO;
import com.realtors.sales.finance.dto.CashFlowStatus;
import com.realtors.sales.finance.dto.CashFlowType;
import com.realtors.sales.rowmapper.SaleRowMapper;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class SaleRepositoryImpl implements SaleRepository {

	private final JdbcTemplate jdbc;
//	private static final Logger logger = LoggerFactory.getLogger(SaleRepositoryImpl.class);
	private static final RowMapper<ReceivableDetailDTO> ROW_MAPPER = new BeanPropertyRowMapper<>(ReceivableDetailDTO.class);
	private static final RowMapper<SaleDetailDTO> SALE_MAPPER = new BeanPropertyRowMapper<>(SaleDetailDTO.class);
	
	@Override
    public List<ReceivableDetailDTO> findSalesByStatus(List<String> statuses) {
        if (statuses == null || statuses.isEmpty()) {
            return List.of();
        }

        String placeholders = String.join( ",", Collections.nCopies(statuses.size(), "?"));
        String sql = "SELECT * FROM v_receivable_details " +
                     "WHERE sale_status IN (" + placeholders + ")";
        return jdbc.query(sql, ROW_MAPPER, statuses.toArray());
    }
	
	@Override
	public SaleDetailDTO getSaleDetails(UUID saleId) {
		String sql = "SELECT * FROM v_receivable_details where sale_id=? ";
		return jdbc.query(sql, SALE_MAPPER, saleId).getFirst();
	}
	
	@Override
	public SaleDTO createSale(UUID plotId, UUID projectId, UUID customerId, UUID soldBy, BigDecimal area,
			BigDecimal basePrice, BigDecimal extraCharges, BigDecimal totalPrice) {
		String sql = """
				    INSERT INTO sales
				    (plot_id, project_id, customer_id, sold_by, area, base_price, extra_charges, total_price, sale_status)
				    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
				    RETURNING sale_id, plot_id, project_id, customer_id, sold_by,
				              area, base_price, extra_charges, total_price, sale_status, confirmed_at
				""";
		return jdbc.queryForObject(sql, new SaleRowMapper(), plotId, projectId, customerId, soldBy, area, basePrice,
				extraCharges, totalPrice, SalesStatus.BOOKED.toString());
	}

	@Override
	public SaleDTO findSaleByPlotId(UUID plotId) {
		String sql = """
				           SELECT sale_id, plot_id, project_id, customer_id, sold_by, area,
				           base_price, extra_charges, total_price, sale_status, confirmed_at
				    FROM sales WHERE plot_id = ?
				""";
		return jdbc.queryForObject(sql, new SaleRowMapper(), plotId);
	}

	@Override
	public SaleDTO findById(UUID saleId) {
		String sql = """
				    SELECT sale_id, plot_id, project_id, customer_id, sold_by, area,
				           base_price, extra_charges, total_price, sale_status, confirmed_at
				    FROM sales WHERE sale_id = ?
				""";
		return jdbc.queryForObject(sql, new SaleRowMapper(), saleId);
	}

	@Override
	public void updateSaleStatus(UUID saleId, String status) {
		String sql = """
				    UPDATE sales
				    SET sale_status = ?, confirmed_at = now(), updated_at = now()
				    WHERE sale_id = ?
				""";
		jdbc.update(sql, status,  saleId);
	}

	public BigDecimal getTotalAmount(UUID saleId) {
		String sql = """
				    SELECT  total_price
				    FROM sales
				    WHERE sale_id = ? AND sale_status <> 'CANCELLED'
				""";
		return jdbc.queryForObject(sql, BigDecimal.class, saleId);
	}
	
	@Override
	public List<CashFlowItemDTO> findReceivables(LocalDate from, LocalDate to) {
		String sql = """
			SELECT s.sale_id, pl.plot_number, c.customer_name AS customer_name, (s.total_price - COALESCE(p.total_received, 0)) AS outstanding
			FROM sales s
			JOIN plot_units pl ON pl.plot_id = s.plot_id
			JOIN customers c ON c.customer_id = s.customer_id
			LEFT JOIN (
				SELECT sale_id, SUM(amount) total_received
				FROM payments
				WHERE payment_type = 'RECEIVED' AND is_verified = true GROUP BY sale_id
			) p ON p.sale_id = s.sale_id
			WHERE (s.total_price - COALESCE(p.total_received, 0)) > 0
			AND s.sale_status <> 'CANCELLED'
		""";

		return jdbc.query(sql, (rs, rowNum) -> {
			CashFlowItemDTO dto = new CashFlowItemDTO();
			dto.setType(CashFlowType.RECEIVABLE);
			dto.setSaleId(rs.getObject("sale_id", UUID.class));
			dto.setPlotNo(rs.getString("plot_number"));
			dto.setPartyName(rs.getString("customer_name"));
			dto.setAmount(rs.getBigDecimal("outstanding"));

			dto.setStatus(
					dto.getAmount().compareTo(BigDecimal.ZERO) > 0
					? CashFlowStatus.OVERDUE
					: CashFlowStatus.DUE
			);

			return dto;
		});
	}
	
	public BigDecimal getOutstandingDueToday() {
		String sql = """
			SELECT COALESCE(SUM(
				s.total_price - COALESCE(p.total_received, 0)
			), 0)
			FROM sales s
			LEFT JOIN (
				SELECT sale_id, SUM(amount) total_received
				FROM payments
				WHERE payment_type = 'RECEIVED'
				GROUP BY sale_id
			) p ON p.sale_id = s.sale_id
		""";

		return jdbc.queryForObject(sql, BigDecimal.class);
	}

	public BigDecimal getTotalSalesAmount() {
	    String sql = """
	        SELECT COALESCE(SUM(total_price), 0)
	        FROM sales
	        WHERE sale_status IN ('BOOKED', 'IN_PROGRESS')
	    """;
	    return jdbc.queryForObject(sql, BigDecimal.class);
	}
	
	@Override
	public List<CashFlowItemDTO> findReceivables(
	        LocalDate from,
	        LocalDate to,
	        CashFlowStatus status
	) {

	    String sql = """
	        SELECT
	            s.sale_id,
	            pl.plot_number,
	            c.customer_name AS customer_name,
	            (s.total_price - COALESCE(p.total_received, 0)) AS outstanding
	        FROM sales s
	        JOIN plot_units pl ON pl.plot_id = s.plot_id
	        JOIN customers c ON c.customer_id = s.customer_id
	        LEFT JOIN (
	            SELECT sale_id, SUM(amount) AS total_received
	            FROM payments
	            WHERE payment_type = 'RECEIVED'
	              AND is_verified = true
	            GROUP BY sale_id
	        ) p ON p.sale_id = s.sale_id
	        WHERE s.sale_status IN ('CONFIRMED','COMPLETED')
	          AND (s.total_price - COALESCE(p.total_received, 0)) > 0
	    """;

	    List<CashFlowItemDTO> items = jdbc.query(
	        sql,
	        (rs, rowNum) -> {
	            CashFlowItemDTO dto = new CashFlowItemDTO();
	            dto.setType(CashFlowType.RECEIVABLE);
	            dto.setSaleId(rs.getObject("sale_id", UUID.class));
	            dto.setPlotNo(rs.getString("plot_number"));
	            dto.setPartyName(rs.getString("customer_name"));
	            dto.setAmount(rs.getBigDecimal("outstanding"));

	            dto.setStatus(
	            		dto.getAmount().compareTo(BigDecimal.ZERO) > 0
	                    ? CashFlowStatus.OVERDUE
	                    : CashFlowStatus.DUE
	            );
	            return dto;
	        }
	    );

	    if (status == null) return items;

	    return items.stream()
	        .filter(i -> i.getStatus() == status)
	        .toList();
	}

	@Override
	public void deleteBySaleId(UUID saleId) {
		String sql = "DELETE FROM sales where sale_id=?";
		jdbc.update(sql, saleId);
	}
}
