package com.realtors.sales.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.realtors.sales.dto.CommissionRuleDTO;
import com.realtors.sales.repository.CommissionRuleRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CommissionRuleService {

    private final CommissionRuleRepository ruleRepo;

    public CommissionRuleDTO addRule(CommissionRuleDTO dto) {
        ruleRepo.insertRule(dto);
        return dto;
    }

    public List<CommissionRuleDTO> listRules(UUID projectId) {
        return ruleRepo.getRulesByProject(projectId).values().stream().toList();
    }
    
    public List<CommissionRuleDTO> findByProjectAndRole(UUID projectId, UUID roleId) {
        return ruleRepo.findByProjectAndRole(projectId, roleId);
    }

    public List<CommissionRuleDTO> findByProject(UUID projectId) {
        return ruleRepo.getRulesByProject(projectId).values().stream().toList();
    }

    public CommissionRuleDTO updateRule(UUID ruleId, CommissionRuleDTO dto) {
        ruleRepo.updateRule(ruleId, dto.getPercentage());
        return dto;
    }

    public void deleteRule(UUID ruleId) {
        ruleRepo.deleteRule(ruleId);
    }
}
