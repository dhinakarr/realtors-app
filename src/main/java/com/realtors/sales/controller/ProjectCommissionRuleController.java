package com.realtors.sales.controller;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.realtors.common.ApiResponse;
import com.realtors.common.util.AppUtil;
import com.realtors.dashboard.dto.UserPrincipalDto;
import com.realtors.sales.dto.CommissionRuleDTO;
import com.realtors.sales.dto.CommissionRulePatchDTO;
import com.realtors.sales.dto.PaymentRuleDetailsDto;
import com.realtors.sales.dto.PaymentRuleDto;
import com.realtors.sales.service.CommissionRuleService;
import com.realtors.sales.service.PaymentRuleService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/commission-rules")
@RequiredArgsConstructor
public class ProjectCommissionRuleController {

    private final CommissionRuleService ruleService;
    private final PaymentRuleService paymentRuleService;
    private static final Logger logger = LoggerFactory.getLogger(ProjectCommissionRuleController.class);

    @PostMapping
    public ResponseEntity<ApiResponse<CommissionRuleDTO>> addRule(@RequestBody CommissionRuleDTO dto) {
    	UUID projectId = dto.getProjectId();
        dto.setProjectId(projectId);
        
        return ResponseEntity.ok(ApiResponse.success("New Rule created", ruleService.addRule(dto), HttpStatus.CREATED));
    }
    
    @PostMapping("/add")
    public ResponseEntity<ApiResponse<PaymentRuleDto>> createRule(@RequestBody PaymentRuleDto dto) {
    	UUID projectId = dto.getProjectId();
        dto.setProjectId(projectId);
        dto.setCreatedBy(AppUtil.getCurrentUserId());
        PaymentRuleDto data = paymentRuleService.createRule(dto);
        return ResponseEntity.ok(ApiResponse.success("New Rule created", data, HttpStatus.CREATED));
    }
    
	@GetMapping("project/{projectId}")
    public ResponseEntity<List<CommissionRuleDTO>> listRules(@PathVariable UUID projectId) {
        return ResponseEntity.ok(ruleService.listRules(projectId));
    }
	
	@GetMapping("/{projectId}")
	public ResponseEntity<ApiResponse<List<PaymentRuleDetailsDto>>> getRulesByProject(@PathVariable UUID projectId) {
		List<PaymentRuleDetailsDto> list = paymentRuleService.listRules(projectId);
        return ResponseEntity.ok(ApiResponse.success("Payment Rules fetched", list, HttpStatus.OK));
    }

    @PatchMapping("/{ruleId}")
    public ResponseEntity<ApiResponse<CommissionRuleDTO>> updateRule(@PathVariable UUID ruleId,
                                                        @RequestBody CommissionRuleDTO dto) {
        return ResponseEntity.ok(ApiResponse.success("List of Rules Fetched", ruleService.updateRule(ruleId, dto)));
    }
    
    @PatchMapping("/rules/{ruleId}")
    public ResponseEntity<ApiResponse<PaymentRuleDto>> patchRule(
            @PathVariable UUID ruleId,
            @RequestBody CommissionRulePatchDTO dto,
            @AuthenticationPrincipal UserPrincipalDto user
    ) {
        dto.setRuleId(ruleId);
        dto.setUpdatedBy(user.getUserId());

        PaymentRuleDto data = paymentRuleService.patchRule(dto).orElse(null);
        return ResponseEntity.ok(ApiResponse.success("Updated successfully", data));
    }
    
    @DeleteMapping("/deactivate/{ruleId}")
    public ResponseEntity<ApiResponse<String>> deActivateRule(@PathVariable UUID ruleId) {
    	paymentRuleService.deactivateRule(ruleId, AppUtil.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success("Deleted", null));
    }

    @DeleteMapping("/{ruleId}")
    public ResponseEntity<ApiResponse<String>> deleteRule(@PathVariable UUID ruleId) {
        ruleService.deleteRule(ruleId);
        return ResponseEntity.ok(ApiResponse.success("Deleted", null));
    }
}

