package com.realtors.dashboard.repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.boot.autoconfigure.security.reactive.PathRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.realtors.dashboard.dto.CommissionDetailsDTO;
import com.realtors.dashboard.dto.PagedResponse;
import com.realtors.dashboard.dto.ReceivableDetailDTO;
import com.realtors.dashboard.dto.SaleDetailDTO;
import com.realtors.dashboard.dto.SiteVisitDetailDTO;
import com.realtors.dashboard.dto.UserPerformanceKpiDTO;
import com.realtors.dashboard.dto.UserPerformanceSnapshotDTO;

@Repository
public class UserPerformanceRepository {

	private final NamedParameterJdbcTemplate jdbc;
	private final JdbcTemplate jdbcTemplate;

	public UserPerformanceRepository(NamedParameterJdbcTemplate jdbc, JdbcTemplate jdbcTemplate) {
		this.jdbc = jdbc;
		this.jdbcTemplate = jdbcTemplate;
	}
	
	public List<SiteVisitDetailDTO> fetchSiteVisits(UUID userId, LocalDate fromDate, LocalDate toDate) {
        String sql = """
            SELECT DISTINCT ON (sv.site_visit_id)
        		    sv.site_visit_id, sv.visit_date, p.project_name,
                   u.full_name AS agent_name,
                   m.full_name AS manager_name,
                   c.customer_name, sv.expense_amount
            FROM site_visits sv
            JOIN projects p ON p.project_id = sv.project_id
            JOIN app_users u ON u.user_id = sv.user_id
            LEFT JOIN app_users m ON m.user_id = u.manager_id
            JOIN site_visit_customers svc ON svc.site_visit_id = sv.site_visit_id
            JOIN customers c ON c.customer_id = svc.customer_id
            WHERE sv.user_id = ?
            AND (CAST(? AS DATE) IS NULL OR sv.visit_date >= CAST(? AS DATE))
        	AND (CAST(? AS DATE) IS NULL OR sv.visit_date <= CAST(? AS DATE))
            ORDER BY sv.site_visit_id, sv.visit_date DESC
        """;

        return jdbcTemplate.query(sql,
                new Object[]{userId, fromDate, fromDate, toDate, toDate},
                new RowMapper<>() {
                    @Override
                    public SiteVisitDetailDTO mapRow(ResultSet rs, int rowNum) throws SQLException {
                        SiteVisitDetailDTO dto = new SiteVisitDetailDTO();
                        dto.setSiteVisitId(UUID.fromString(rs.getString("site_visit_id")));
                        dto.setVisitDate(rs.getDate("visit_date").toLocalDate());
                        dto.setProjectName(rs.getString("project_name"));
                        dto.setAgentName(rs.getString("agent_name"));
                        dto.setManagerName(rs.getString("manager_name"));
                        dto.setCustomerName(rs.getString("customer_name"));
                        dto.setExpenseAmount(rs.getBigDecimal("expense_amount"));
                        return dto;
                    }
                });
    }

    public List<SaleDetailDTO> fetchSales(UUID userId, LocalDate fromDate, LocalDate toDate) {
        String sql = """
            SELECT s.sale_id, p.project_name, pl.plot_number, c.customer_name,
                   u.full_name AS agent_name, m.full_name AS manager_name,
                   s.total_price, s.base_price, s.confirmed_at
            FROM sales s
            JOIN plot_units pl ON pl.plot_id = s.plot_id
            JOIN projects p ON p.project_id = s.project_id
            JOIN customers c ON c.customer_id = s.customer_id
            JOIN app_users u ON u.user_id = s.sold_by
            LEFT JOIN app_users m ON m.user_id = u.manager_id
            WHERE s.sold_by = ?
            AND (CAST(? AS DATE) IS NULL OR s.confirmed_at >= CAST(? AS DATE))
        	AND (CAST(? AS DATE) IS NULL OR s.confirmed_at <= CAST(? AS DATE))
            ORDER BY s.confirmed_at DESC
        """;

        return jdbcTemplate.query(sql,
                new Object[]{userId, fromDate, fromDate, toDate, toDate},
                new RowMapper<>() {
                    @Override
                    public SaleDetailDTO mapRow(ResultSet rs, int rowNum) throws SQLException {
                        SaleDetailDTO dto = new SaleDetailDTO();
                        dto.setSaleId(UUID.fromString(rs.getString("sale_id")));
                        dto.setProjectName(rs.getString("project_name"));
                        dto.setPlotNumber(rs.getString("plot_number"));
                        dto.setCustomerName(rs.getString("customer_name"));
                        dto.setAgentName(rs.getString("agent_name"));
                        dto.setSaleAmount(rs.getBigDecimal("total_price"));
                        dto.setBaseAmount(rs.getBigDecimal("base_price"));
                        dto.setConfirmedAt(rs.getTimestamp("confirmed_at") != null
                                ? rs.getTimestamp("confirmed_at").toLocalDateTime().toLocalDate() : null);
                        return dto;
                    }
                });
    }

    public List<ReceivableDetailDTO> fetchReceivable(UUID userId, LocalDate fromDate, LocalDate toDate) {
        String sql = """
            SELECT p.payment_id, proj.project_name, pl.plot_number, c.customer_name, s.total_price, s.base_price,
                   p.amount, p.payment_date
            FROM payments p
            JOIN sales s ON s.sale_id = p.sale_id
            JOIN plot_units pl ON pl.plot_id = s.plot_id
            JOIN projects proj ON proj.project_id = s.project_id
            JOIN customers c ON c.customer_id = s.customer_id
            WHERE s.sold_by = ?
            AND payment_type='RECEIVED'
            AND (CAST(? AS DATE) IS NULL OR p.payment_date >= CAST(? AS DATE))
        	AND (CAST(? AS DATE) IS NULL OR p.payment_date <= CAST(? AS DATE))
            ORDER BY p.payment_date DESC
        """;

        return jdbcTemplate.query(sql,
                new Object[]{userId, fromDate, fromDate, toDate, toDate},
                (rs, rowNum) -> {
                    ReceivableDetailDTO dto = new ReceivableDetailDTO();
                    dto.setProjectName(rs.getString("project_name"));
                    dto.setPlotNumber(rs.getString("plot_number"));
                    dto.setCustomerName(rs.getString("customer_name"));
                    dto.setSaleAmount(rs.getBigDecimal("total_price"));
                    dto.setBaseAmount(rs.getBigDecimal("base_price"));
                    dto.setTotalReceived(rs.getBigDecimal("amount"));
                    dto.setConfirmedAt(rs.getDate("payment_date").toLocalDate());
                    return dto;
                });
    }

    public List<CommissionDetailsDTO> fetchCommission(UUID userId, LocalDate fromDate, LocalDate toDate) {
        String sql = """
            SELECT sc.commission_id, proj.project_name, sc.agent_name, sc.sale_amount, sc.base_amount, sc.commission_paid, 
                   sc.total_commission,  sc.confirmed_at
            FROM v_commission_payable_details sc
            JOIN projects proj ON proj.project_id = sc.project_id
            JOIN app_users u ON u.user_id = sc.agent_id
            WHERE sc.agent_id = ?
            AND (CAST(? AS DATE) IS NULL OR sc.confirmed_at >= CAST(? AS DATE))
        	AND (CAST(? AS DATE) IS NULL OR sc.confirmed_at <= CAST(? AS DATE))
            ORDER BY sc.confirmed_at DESC
        """;

        return jdbcTemplate.query(sql,
                new Object[]{userId, fromDate, fromDate, toDate, toDate},
                (rs, rowNum) -> {
                    CommissionDetailsDTO dto = new CommissionDetailsDTO();
                    dto.setCommissionId(UUID.fromString(rs.getString("commission_id")));
                    dto.setAgentName(rs.getString("agent_name"));
                    dto.setProjectName(rs.getString("project_name"));
                    dto.setSaleAmount(rs.getBigDecimal("sale_amount"));
                    dto.setBaseAmount(rs.getBigDecimal("base_amount"));
                    dto.setTotalCommission(rs.getBigDecimal("total_commission"));
                    dto.setCommissionPaid(rs.getBigDecimal("commission_paid"));
//                    dto.setIsReleased(rs.getBoolean("is_released"));
					dto.setConfirmedAt(rs.getDate("confirmed_at") != null ? rs.getDate("confirmed_at").toLocalDate() : null);
					 
                    return dto;
                });
    }
	

	public List<UserPerformanceSnapshotDTO> fetchSnapshot(List<UUID> userIds) {

		if (userIds == null || userIds.isEmpty()) {
			throw new IllegalArgumentException("userIds must not be empty");
		}

		String sql = """
				    SELECT
				        user_id, full_name, manager_id, manager_name, total_site_visits, total_customers, total_sales,
				        sales_value, total_received, total_outstanding, commission_earned, commission_paid, commission_payable
				    FROM v_user_performance_summary
				    WHERE user_id IN (:userIds)
				""";

		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue("userIds", userIds == null || userIds.isEmpty() ? null : userIds);

		return jdbc.query(sql, params, userPerformanceRowMapper());
	}

	private RowMapper<UserPerformanceSnapshotDTO> userPerformanceRowMapper() {
		return (rs, rowNum) -> {
			UserPerformanceSnapshotDTO dto = new UserPerformanceSnapshotDTO();

			dto.setUserId(rs.getObject("user_id", UUID.class));
			dto.setFullName(rs.getString("full_name"));
			dto.setManagerId(rs.getObject("manager_id", UUID.class));
			dto.setManagerName(rs.getString("manager_name"));

			dto.setTotalSiteVisits(rs.getLong("total_site_visits"));
			dto.setTotalCustomers(rs.getLong("total_customers"));

			dto.setTotalSales(rs.getLong("total_sales"));
			dto.setSalesValue(rs.getBigDecimal("sales_value"));
			dto.setTotalReceived(rs.getBigDecimal("total_received"));
			dto.setTotalOutstanding(rs.getBigDecimal("total_outstanding"));

			dto.setCommissionEarned(rs.getBigDecimal("commission_earned"));
			dto.setCommissionPaid(rs.getBigDecimal("commission_paid"));
			dto.setCommissionPayable(rs.getBigDecimal("commission_payable"));

			return dto;
		};
	}

	public List<SaleDetailDTO> fetchSalesDetails(UUID userId, LocalDate fromDate, LocalDate toDate) {

		String sql = """
				    SELECT *
				    FROM v_receivable_details
				    WHERE agent_id = :userId
				      AND (:fromDate IS NULL OR confirmed_at >= :fromDate)
				      AND (:toDate IS NULL OR confirmed_at <= :toDate)
				""";

		MapSqlParameterSource params = new MapSqlParameterSource().addValue("userId", userId)
				.addValue("fromDate", fromDate).addValue("toDate", toDate);

		return jdbc.query(sql, params, saleDetailRowMapper());
	}

	private RowMapper<SaleDetailDTO> saleDetailRowMapper() {
		return (rs, rowNum) -> {
			SaleDetailDTO dto = new SaleDetailDTO();
			dto.setSaleId(rs.getObject("saleId", UUID.class));
			dto.setProjectId(rs.getObject("projectId", UUID.class));
			dto.setProjectName(rs.getString("projectName"));
			dto.setPlotNumber(rs.getString("plotNumber"));
			dto.setSaleAmount(rs.getBigDecimal("saleAmount"));
			dto.setReceivedAmount(rs.getBigDecimal("receivedAmount"));
			dto.setOutstandingAmount(rs.getBigDecimal("outstandingAmount"));
			dto.setSaleStatus(rs.getString("saleStatus"));
			dto.setConfirmedAt(rs.getObject("confirmedAt", LocalDate.class));
			return dto;
		};
	}

	public UserPerformanceKpiDTO fetchKpis(List<UUID> userIds, UUID projectId, OffsetDateTime from, OffsetDateTime to) {
		StringBuilder sql = new StringBuilder("""
			    SELECT
			        COALESCE(SUM(site_visits), 0)        AS total_site_visits,
			        COALESCE(COUNT(DISTINCT sale_id), 0) AS total_sales,
			        COALESCE(SUM(total_received), 0)     AS total_received,
			        COALESCE(SUM(commission_earned), 0)  AS total_commission
			    FROM v_user_performance_core
			    WHERE user_id IN (:userIds)
			""");

			MapSqlParameterSource params = new MapSqlParameterSource()
			        .addValue("userIds", userIds);

			if (projectId != null) {
			    sql.append(" AND project_id = :projectId");
			    params.addValue("projectId", projectId);
			}

			if (from != null) {
			    sql.append(" AND activity_date >= :fromDate");
			    params.addValue("fromDate", from);
			}

			if (to != null) {
			    sql.append(" AND activity_date <= :toDate");
			    params.addValue("toDate", to);
			}

		return jdbc.query(sql.toString(), params, rs -> {
		    if (!rs.next()) {
		        return new UserPerformanceKpiDTO(0, 0, BigDecimal.ZERO, BigDecimal.ZERO);
		    }
		    return new UserPerformanceKpiDTO(
		            rs.getInt("total_site_visits"),
		            rs.getInt("total_sales"),
		            rs.getBigDecimal("total_received"),
		            rs.getBigDecimal("total_commission")
		    );
		});
	}

	private MapSqlParameterSource baseParams(List<UUID> userIds, UUID projectId, OffsetDateTime from, OffsetDateTime to) {
		if (userIds == null || userIds.isEmpty()) {
			throw new IllegalArgumentException("userIds must not be empty");
		}
		return new MapSqlParameterSource().addValue("userIds", userIds).addValue("projectId", projectId)
				.addValue("fromDate", from).addValue("toDate", to);
	}

	public PagedResponse<UserPerformanceSnapshotDTO> fetchSnapshotPage(
	        List<UUID> userIds,
	        UUID projectId,
	        OffsetDateTime from,
	        OffsetDateTime to,
	        int page,
	        int size
	) {
	    int offset = page * size;

	    StringBuilder where = new StringBuilder("""
	        WHERE user_id IN (:userIds)
	    """);

	    MapSqlParameterSource params = new MapSqlParameterSource();
	    params.addValue("userIds", userIds);

	    if (projectId != null) {
	        where.append(" AND project_id = :projectId");
	        params.addValue("projectId", projectId);
	    }

	    if (from != null) {
	        where.append(" AND activity_date >= :fromDate");
	        params.addValue("fromDate", from);
	    }

	    if (to != null) {
	        where.append(" AND activity_date <= :toDate");
	        params.addValue("toDate", to);
	    }

	    String dataSql = """
	        SELECT user_id, full_name, manager_id, manager_name, site_visits, sales_value, total_received, commission_earned
	        FROM v_user_performance_core
	        %s
	        ORDER BY sales_value DESC
	        LIMIT :limit OFFSET :offset
	    """.formatted(where);

	    String countSql = """
	        SELECT COUNT(*)
	        FROM v_user_performance_summary
	        %s
	    """.formatted(where);

	    params.addValue("limit", size);
	    params.addValue("offset", offset);

	    List<UserPerformanceSnapshotDTO> rows =
	            jdbc.query(dataSql, params, snapshotRowMapper());

	    long total =
	            jdbc.queryForObject(countSql, params, Long.class);

	    PagedResponse<UserPerformanceSnapshotDTO> response = new PagedResponse<>();
	    response.setData(rows);
	    response.setTotal(total);
	    response.setPage(page);
	    response.setSize(size);
	    response.setTotalPages((int) Math.ceil((double) total / size));

	    return response;
	}


	private RowMapper<UserPerformanceSnapshotDTO> snapshotRowMapper() {
	    return (rs, rowNum) -> {
	        UserPerformanceSnapshotDTO dto = new UserPerformanceSnapshotDTO();
	        dto.setUserId(UUID.fromString(rs.getString("user_id")));
	        dto.setFullName(rs.getString("full_name"));
	        dto.setManagerId((UUID) rs.getObject("manager_id"));
	        dto.setManagerName(rs.getString("manager_name"));
	        dto.setTotalSiteVisits(rs.getInt("site_visits"));
	        dto.setTotalSales(rs.getInt("sales_value"));
	        dto.setTotalReceived(rs.getBigDecimal("total_received"));
	        dto.setCommissionEarned(rs.getBigDecimal("commission_earned"));
	        return dto;
	    };
	}

}
