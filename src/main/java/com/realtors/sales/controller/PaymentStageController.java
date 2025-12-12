package com.realtors.sales.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.realtors.common.ApiResponse;
import com.realtors.sales.dto.PaymentStageDTO;
import com.realtors.sales.service.PaymentStageService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/payment-stages")
@RequiredArgsConstructor
public class PaymentStageController {

    private final PaymentStageService stageService;

    @PostMapping
    public ResponseEntity<ApiResponse<PaymentStageDTO>> addStage(@RequestBody PaymentStageDTO dto) {
    	UUID projectId = dto.getProjectId();
        dto.setProjectId(projectId);
        PaymentStageDTO data = stageService.addStage(dto);
        return ResponseEntity.ok(ApiResponse.success("Fetched all Payment stages", data));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<PaymentStageDTO>>> listStages(@PathVariable UUID projectId) {
        return ResponseEntity.ok(ApiResponse.success("Fetched all Payment stages",stageService.listStages(projectId)));
    }

    @PatchMapping("/{stageId}")
    public ResponseEntity<ApiResponse<Void>> updateStage(@PathVariable UUID stageId,
                                                       @RequestBody PaymentStageDTO dto) {
    	stageService.updateStage(stageId, dto);
        return ResponseEntity.ok(ApiResponse.success("Data updated successfully", null));
    }

    @DeleteMapping("/{stageId}")
    public ResponseEntity<String> deleteStage(@PathVariable UUID stageId) {
//        stageService.deleteStage(stageId);
        return ResponseEntity.ok("Deleted");
    }
}

