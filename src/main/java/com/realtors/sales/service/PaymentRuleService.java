package com.realtors.sales.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestMapping;

import com.realtors.common.EnumConstants;
import com.realtors.common.service.AuditTrailService;
import com.realtors.common.util.AppUtil;
import com.realtors.sales.dto.CommissionRulePatchDTO;
import com.realtors.sales.dto.PaymentRuleDetailsDto;
import com.realtors.sales.dto.PaymentRuleDto;
import com.realtors.sales.repository.PaymentRuleRepository;

import lombok.RequiredArgsConstructor;

@Service
public class PaymentRuleService {

	private final PaymentRuleRepository dao;
	private final AuditTrailService audit;
    private String TABLE_NAME = "payment_commission_rules";

	public PaymentRuleService(PaymentRuleRepository dao, AuditTrailService audit) {
		this.dao = dao;
		this.audit = audit;
	}

	public PaymentRuleDto createRule(PaymentRuleDto rule) {
		UUID ruleId = dao.create(rule);
		audit.auditAsync(TABLE_NAME, ruleId, EnumConstants.CREATE);
		return getRule(ruleId);
	}

	public PaymentRuleDto getRule(UUID ruleId) {
		return dao.findById(ruleId).orElseThrow(() -> new RuntimeException("Rule not found"));
	}

	public List<PaymentRuleDetailsDto> listRules(UUID projectId) {
		return dao.findByProject(projectId);
	}

	public void updateRule(PaymentRuleDto rule) {
		audit.auditAsync(TABLE_NAME, rule.getRuleId(), EnumConstants.UPDATE);
		dao.update(rule);
	}

	public void deactivateRule(UUID ruleId, UUID userId) {
		audit.auditAsync(TABLE_NAME, ruleId, EnumConstants.DELETE);
		dao.deactivate(ruleId, userId);
	}
	
	public Optional<PaymentRuleDto> patchRule(CommissionRulePatchDTO patch) {
        if (patch.getRuleId() == null) {
            throw new IllegalArgumentException("ruleId is required for patch update");
        }

        if (patch.getUpdatedBy() == null) {
            throw new IllegalArgumentException("updatedBy is required");
        }
        audit.auditAsync(TABLE_NAME, patch.getRuleId(), EnumConstants.PATCH);
       return  dao.patchUpdate(patch);
    }
}
