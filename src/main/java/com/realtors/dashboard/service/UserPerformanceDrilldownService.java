package com.realtors.dashboard.service;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.realtors.dashboard.dto.PagedResponse;
import com.realtors.dashboard.dto.SaleDetailDTO;
import com.realtors.dashboard.repository.SalesDrilldownRepository;

@Service
public class UserPerformanceDrilldownService {

	private final SalesDrilldownRepository salesRepo;

	public UserPerformanceDrilldownService(SalesDrilldownRepository salesRepo) {
		this.salesRepo = salesRepo;
	}

	public PagedResponse<SaleDetailDTO> getSalesDetails(UUID userId, UUID projectId, LocalDate fromDate,
			LocalDate toDate, int page, int size) {

		// defensive defaults
		int safePage = Math.max(page, 0);
		int safeSize = Math.min(Math.max(size, 1), 100);

		return salesRepo.fetchSales(userId, projectId, fromDate, toDate, safePage, safeSize);
	}
}
