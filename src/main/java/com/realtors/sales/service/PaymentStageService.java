package com.realtors.sales.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.realtors.sales.dto.PaymentStageDTO;
import com.realtors.sales.repository.PaymentStageRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PaymentStageService {
	
	private final PaymentStageRepository stageRepo;
	
	public PaymentStageDTO addStage(PaymentStageDTO dto) {
		return stageRepo.insertStage(dto);
	}

	public List<PaymentStageDTO> listStages(UUID projectId) {
		return stageRepo.listStages(projectId);
	}
	
	public void updateStage(UUID stageId, PaymentStageDTO dto) {
		stageRepo.updateStage(stageId, dto);
	}
}
