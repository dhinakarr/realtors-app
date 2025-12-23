package com.realtors.dashboard.service;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.realtors.dashboard.dto.ReceivableDashboardDTO;
import com.realtors.dashboard.repository.ReceivableRepository;

@Service
public class ReceivableService {

	private final ReceivableRepository receivableRepository;

	public ReceivableService(ReceivableRepository receivableRepository) {
		this.receivableRepository = receivableRepository;
	}

	public ReceivableDashboardDTO getReceivableDashboard(UUID agentId) {
		ReceivableDashboardDTO dto = new ReceivableDashboardDTO();
		dto.setSummary(receivableRepository.getSummary());
		dto.setReceivables(receivableRepository.getReceivables(agentId));
		return dto;
	}
}
