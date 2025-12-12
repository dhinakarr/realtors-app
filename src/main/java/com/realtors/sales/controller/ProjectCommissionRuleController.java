package com.realtors.sales.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.realtors.common.ApiResponse;
import com.realtors.sales.dto.CommissionRuleDTO;
import com.realtors.sales.service.CommissionRuleService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/commission-rules")
@RequiredArgsConstructor
public class ProjectCommissionRuleController {

    private final CommissionRuleService ruleService;

    @PostMapping
    public ResponseEntity<ApiResponse<CommissionRuleDTO>> addRule(@RequestBody CommissionRuleDTO dto) {
    	UUID projectId = dto.getProjectId();
        dto.setProjectId(projectId);
        return ResponseEntity.ok(ApiResponse.success("New Rule created", ruleService.addRule(dto), HttpStatus.CREATED));
    }
    
	/*
	 * @GetMapping("/project/{projectId}") public
	 * ResponseEntity<ApiResponse<List<CommissionRuleDTO>>>
	 * getRulesByProjectAndRole(
	 * 
	 * @PathVariable UUID projectId,
	 * 
	 * @RequestParam(required = false) UUID roleId ) { List<CommissionRuleDTO>
	 * rules;
	 * 
	 * if (roleId != null) { rules = ruleService.findByProjectAndRole(projectId,
	 * roleId); } else { rules = ruleService.findByProject(projectId); }
	 * 
	 * return ResponseEntity.ok(ApiResponse.success("List of Rules Fetched", rules,
	 * HttpStatus.OK)); }
	 */

    @GetMapping("project/{projectId}")
    public ResponseEntity<List<CommissionRuleDTO>> listRules(@PathVariable UUID projectId) {
        return ResponseEntity.ok(ruleService.listRules(projectId));
    }

    @PatchMapping("/{ruleId}")
    public ResponseEntity<ApiResponse<CommissionRuleDTO>> updateRule(@PathVariable UUID ruleId,
                                                        @RequestBody CommissionRuleDTO dto) {
        return ResponseEntity.ok(ApiResponse.success("List of Rules Fetched", ruleService.updateRule(ruleId, dto)));
    }

    @DeleteMapping("/{ruleId}")
    public ResponseEntity<ApiResponse<String>> deleteRule(@PathVariable UUID ruleId) {
        ruleService.deleteRule(ruleId);
        return ResponseEntity.ok(ApiResponse.success("Deleted", null));
    }
}

