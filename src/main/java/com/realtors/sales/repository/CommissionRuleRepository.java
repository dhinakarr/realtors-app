package com.realtors.sales.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.realtors.sales.dto.CommissionRuleDTO;

public interface CommissionRuleRepository {
    Map<UUID, CommissionRuleDTO> getRulesByProject(UUID projectId);
    public void insertRule(CommissionRuleDTO dto);
    public void updateRule(UUID ruleId, BigDecimal percentage);
    public void deleteRule(UUID ruleId);
    List<CommissionRuleDTO> findByProjectAndRole(UUID projectId, UUID roleId);
    List<CommissionRuleDTO> findByProject(UUID projectId);
}

