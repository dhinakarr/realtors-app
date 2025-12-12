package com.realtors.sales.repository;

import java.util.List;
import java.util.UUID;

import com.realtors.sales.dto.PaymentStageDTO;

public interface PaymentStageRepository {
	
	public PaymentStageDTO insertStage(PaymentStageDTO dto);
	public List<PaymentStageDTO> listStages(UUID projectId);
	public void updateStage(UUID stageId, PaymentStageDTO dto);
	public void deleteStage(UUID stageId);

}
