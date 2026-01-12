package com.realtors.sales.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.realtors.common.util.AppUtil;
import com.realtors.sales.dto.CommissionRuleDTO;
import com.realtors.sales.rowmapper.CommissionRuleRowMapper;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class CommissionRuleRepositoryImpl implements CommissionRuleRepository {

    private final JdbcTemplate jdbc;

    @Override
    public Map<UUID, CommissionRuleDTO> getRulesByProject(UUID projectId) {
        String sql = """
            SELECT cr.rule_id, cr.project_id, cr.role_id, cr.percentage,
			       r.role_name
			FROM commission_rules cr
			JOIN roles r ON cr.role_id = r.role_id
			WHERE cr.project_id=?
        """;
        List<CommissionRuleDTO> list = jdbc.query(sql, new CommissionRuleRowMapper(), projectId);
        return list.stream()
                .collect(Collectors.toMap(CommissionRuleDTO::getRoleId, x -> x));
    }
    
    @Override
    public void insertRule(CommissionRuleDTO dto) {

        String sql = """
            INSERT INTO commission_rules
            (project_id, role_id, percentage, created_by, updated_by)
            VALUES (?, ?, ?, ?, ?)
        """;
        UUID userId = AppUtil.getCurrentUserId();
        jdbc.update(sql, dto.getProjectId(), dto.getRoleId(), 
        				dto.getPercentage(), userId, userId);
    }
    
    @Override
    public void updateRule(UUID ruleId, BigDecimal percentage) {

        String sql = """
            UPDATE commission_rules 
            SET percentage = ?, updated_by = ?
            WHERE rule_id = ?
        """;
        UUID userId = AppUtil.getCurrentUserId();
        jdbc.update(sql, percentage, userId, ruleId);
    }
    
    @Override
    public void deleteRule(UUID ruleId) {

        String sql = "DELETE FROM commission_rules WHERE rule_id = ?";

        jdbc.update(sql, ruleId);
    }
    
    @Override
    public List<CommissionRuleDTO> findByProjectAndRole(UUID projectId, UUID roleId) {
        String sql = """
            SELECT cr.rule_id, cr.project_id, cr.role_id, cr.percentage,
                   r.role_name AS role_name
            FROM commission_rules cr
            JOIN roles r ON cr.role_id = r.role_id
            WHERE cr.project_id = ? AND cr.role_id = ?
            ORDER BY r.role_name
        """;

        return jdbc.query(sql, new CommissionRuleRowMapper(), projectId, roleId);
    }

    @Override
    public List<CommissionRuleDTO> findByProject(UUID projectId) {
        String sql = """
            SELECT cr.rule_id, cr.project_id, cr.role_id, cr.percentage,
                   r.role_name AS role_name
            FROM commission_rules cr
            JOIN roles r ON cr.role_id = r.role_id
            WHERE cr.project_id = ?
            ORDER BY r.role_name
        """;

        return jdbc.query(sql, new CommissionRuleRowMapper(), projectId);
    }
}

