package com.realtors.sales.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.realtors.common.util.AppUtil;
import com.realtors.sales.dto.CommissionRulePatchDTO;
import com.realtors.sales.dto.PaymentRuleDetailsDto;
import com.realtors.sales.dto.PaymentRuleDto;
import com.realtors.sales.repository.PaymentRuleRepository;

@Service
public class PaymentRuleService {

	private final PaymentRuleRepository dao;

	public PaymentRuleService(PaymentRuleRepository dao) {
		this.dao = dao;
	}

	public PaymentRuleDto createRule(PaymentRuleDto rule) {
		UUID ruleId = dao.create(rule);
		return getRule(ruleId);
	}

	public PaymentRuleDto getRule(UUID ruleId) {
		return dao.findById(ruleId).orElseThrow(() -> new RuntimeException("Rule not found"));
	}

	public List<PaymentRuleDetailsDto> listRules(UUID projectId) {
		return dao.findByProject(projectId);
	}

	public void updateRule(PaymentRuleDto rule) {
		dao.update(rule);
	}

	public void deactivateRule(UUID ruleId, UUID userId) {
		dao.deactivate(ruleId, userId);
	}
	
	public Optional<PaymentRuleDto> patchRule(CommissionRulePatchDTO patch) {
        if (patch.getRuleId() == null) {
            throw new IllegalArgumentException("ruleId is required for patch update");
        }

        if (patch.getUpdatedBy() == null) {
            throw new IllegalArgumentException("updatedBy is required");
        }

       return  dao.patchUpdate(patch);
    }
}
