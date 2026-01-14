package com.realtors.sales.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import com.realtors.sales.dto.CommissionRulePatchDTO;
import com.realtors.sales.dto.CommissionSpreadRuleDTO;
import com.realtors.sales.dto.PaymentRuleDetailsDto;
import com.realtors.sales.dto.PaymentRuleDto;
import com.realtors.sales.rowmapper.PaymentRuleRowMapper;

@Repository
public class PaymentRuleRepository {

	private final JdbcTemplate jdbcTemplate;
	private final PaymentRuleRowMapper mapper;
	private static final RowMapper<PaymentRuleDetailsDto> ROW_MAPPER = new BeanPropertyRowMapper<>(PaymentRuleDetailsDto.class);
	private static final RowMapper<CommissionSpreadRuleDTO> SPREAD_ROW_MAPPER = new BeanPropertyRowMapper<>(CommissionSpreadRuleDTO.class);
	private static final Logger logger = LoggerFactory.getLogger(PaymentRuleRepository.class);

	public PaymentRuleRepository(JdbcTemplate jdbcTemplate, PaymentRuleRowMapper mapper) {
		this.jdbcTemplate = jdbcTemplate;
		this.mapper = mapper;
	}

	/* CREATE */
	public UUID create(PaymentRuleDto r) {
		UUID id = UUID.randomUUID();
		jdbcTemplate.update("""
				    INSERT INTO payment_commission_rules (
				        rule_id, project_id, role_id, user_id,
				        commission_type, commission_value,
				        priority, active,
				        effective_from, 
				        created_by
				    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
				""", id, r.getProjectId(), r.getRoleId(), r.getUserId(), r.getCommissionType(), r.getCommissionValue(),
				r.getPriority(), r.getActive(), r.getEffectiveFrom(), r.getCreatedBy());

		return id;
	}

	/* READ BY ID */
	public Optional<PaymentRuleDto> findById(UUID ruleId) {
		return jdbcTemplate.query("""
				    SELECT * FROM payment_commission_rules WHERE rule_id = ?
				""", mapper, ruleId).stream().findFirst();
	}

	/* READ ALL (project scoped) */
	public List<PaymentRuleDetailsDto> findByProject(UUID projectId) {
		String sql = """
				SELECT pc.*,  r.role_name, u.full_name AS user_name
				FROM payment_commission_rules pc
				JOIN roles r
				  ON r.role_id = pc.role_id
				LEFT JOIN app_users u
				  ON u.user_id = pc.user_id
				WHERE pc.project_id =? AND pc.active=true
				ORDER BY pc.priority, pc.effective_from;
				""";
		return jdbcTemplate.query(sql, ROW_MAPPER, projectId);
	}
	
	public List<CommissionSpreadRuleDTO> findActiveRulesByProject(UUID projectId) {
		
		String sql = """
				SELECT cr.rule_id, cr.role_id, cr.user_id, r.role_name, r.role_level, cr.commission_type, cr.commission_value
				FROM payment_commission_rules cr
				JOIN roles r ON r.role_id = cr.role_id
				WHERE cr.project_id = ? AND cr.active=true
				""";
		
		return jdbcTemplate.query(sql, SPREAD_ROW_MAPPER, projectId);
	}

	/* UPDATE */
	public void update(PaymentRuleDto r) {
		jdbcTemplate.update("""
				    UPDATE payment_commission_rules
				    SET
				        role_id = ?,
				        user_id = ?,
				        commission_type = ?,
				        commission_value = ?,
				        priority = ?,
				        active = ?,
				        effective_from = ?,
				        effective_to = ?,
				        updated_at = CURRENT_TIMESTAMP,
				        updated_by = ?
				    WHERE rule_id = ?
				""", r.getRoleId(), r.getUserId(), r.getCommissionType(), r.getCommissionValue(), r.getPriority(),
				r.getActive(), r.getEffectiveFrom(), r.getEffectiveTo(), r.getUpdatedBy(), r.getRuleId());
	}

	/* DELETE (SOFT DELETE) */
	public void deactivate(UUID ruleId, UUID userId) {
		jdbcTemplate.update("""
				    UPDATE payment_commission_rules
				    SET active = FALSE,
				        updated_at = CURRENT_TIMESTAMP,
				        updated_by = ?
				    WHERE rule_id = ?
				""", userId, ruleId);
	}
	
	public Optional<PaymentRuleDto> patchUpdate(CommissionRulePatchDTO p) {

        StringBuilder sql = new StringBuilder("""
            UPDATE payment_commission_rules SET
        """);

        List<Object> params = new ArrayList<>();

        if (p.getRoleId() != null) {
            sql.append(" role_id = ?,");
            params.add(p.getRoleId());
        }
        if (p.getUserId() != null) {
            sql.append(" user_id = ?,");
            params.add(p.getUserId());
        }
        if (p.getCommissionType() != null) {
            sql.append(" commission_type = ?,");
            params.add(p.getCommissionType());
        }
        if (p.getCommissionValue() != null) {
            sql.append(" commission_value = ?,");
            params.add(p.getCommissionValue());
        }
        if (p.getPriority() != null) {
            sql.append(" priority = ?,");
            params.add(p.getPriority());
        }
        if (p.getActive() != null) {
            sql.append(" active = ?,");
            params.add(p.getActive());
        }
        if (p.getEffectiveFrom() != null) {
            sql.append(" effective_from = ?,");
            params.add(p.getEffectiveFrom());
        }
        if (p.getEffectiveTo() != null) {
            sql.append(" effective_to = ?,");
            params.add(p.getEffectiveTo());
        }

        // Always update audit columns
        sql.append(" updated_at = CURRENT_TIMESTAMP, updated_by = ?");
        params.add(p.getUpdatedBy());

        sql.append(" WHERE rule_id = ?");
        params.add(p.getRuleId());

        jdbcTemplate.update(sql.toString(), params.toArray());
        return findById(p.getRuleId());
    }
}
