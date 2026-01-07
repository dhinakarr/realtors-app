package com.realtors.dashboard.repository;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.realtors.dashboard.dto.PagedResponse;
import com.realtors.dashboard.dto.SaleDetailDTO;

@Repository
public class SalesDrilldownRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public SalesDrilldownRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final String BASE_WHERE = """
        FROM v_receivable_details
        WHERE agent_id = :userId
          AND (:projectId IS NULL OR project_id = :projectId)
          AND (:fromDate IS NULL OR confirmed_at >= :fromDate)
          AND (:toDate IS NULL OR confirmed_at <= :toDate)
        """;

    private static final String DATA_SQL = """
        SELECT
            sale_id,
            project_id,
            project_name,
            plot_number,
            sale_amount,
            total_received,
            outstanding_amount,
            sale_status,
            confirmed_at
        """ + BASE_WHERE + """
        ORDER BY confirmed_at DESC
        LIMIT :limit OFFSET :offset
        """;

    private static final String COUNT_SQL =
        "SELECT COUNT(*) " + BASE_WHERE;

    public PagedResponse<SaleDetailDTO> fetchSales(
            UUID userId,
            UUID projectId,
            LocalDate fromDate,
            LocalDate toDate,
            int page,
            int size
    ) {
        int offset = page * size;
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("userId", userId)
            .addValue("projectId", projectId)
            .addValue("fromDate", fromDate)
            .addValue("toDate", toDate)
            .addValue("limit", size)
            .addValue("offset", offset);

        List<SaleDetailDTO> data = jdbc.query(DATA_SQL, params, saleRowMapper());
        long total = jdbc.queryForObject(COUNT_SQL, params, Long.class);
        PagedResponse<SaleDetailDTO> response = new PagedResponse<>();
        response.setData(data);
        response.setTotal(total);
        response.setPage(page);
        response.setSize(size);

        return response;
    }

    private RowMapper<SaleDetailDTO> saleRowMapper() {
        return (rs, rowNum) -> {
            SaleDetailDTO dto = new SaleDetailDTO();
            dto.setSaleId(rs.getObject("sale_id", UUID.class));
            dto.setProjectId(rs.getObject("project_id", UUID.class));
            dto.setProjectName(rs.getString("project_name"));
            dto.setPlotNumber(rs.getString("plot_number"));
            dto.setSaleAmount(rs.getBigDecimal("sale_amount"));
            dto.setReceivedAmount(rs.getBigDecimal("total_received"));
            dto.setOutstandingAmount(rs.getBigDecimal("outstanding_amount"));
            dto.setSaleStatus(rs.getString("sale_status"));
            dto.setConfirmedAt(
                rs.getObject("confirmed_at", LocalDate.class)
            );
            return dto;
        };
    }
}
