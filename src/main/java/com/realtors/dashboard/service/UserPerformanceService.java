package com.realtors.dashboard.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.realtors.admin.service.UserHierarchyService;
import com.realtors.admin.service.UserService;
import com.realtors.dashboard.dto.PagedResponse;
import com.realtors.dashboard.dto.PerformanceResponse;
import com.realtors.dashboard.dto.SiteVisitDetailDTO;
import com.realtors.dashboard.dto.UserPerformanceDTO;
import com.realtors.dashboard.dto.UserPerformanceKpiDTO;
import com.realtors.dashboard.dto.UserPerformanceResponse;
import com.realtors.dashboard.dto.UserPerformanceSnapshotDTO;
import com.realtors.dashboard.repository.UserPerformanceRepository;

@Service
public class UserPerformanceService {

	private final UserPerformanceRepository repo;
	private UserHierarchyService hierarchyService;
	private UserService userRepo;

	public UserPerformanceService(UserPerformanceRepository repo, UserHierarchyService hierarchyService, UserService userRepo) {
		this.repo = repo;
		this.hierarchyService = hierarchyService;
		this.userRepo = userRepo;
	}

	public PerformanceResponse getSnapshot(UUID userId) {
		
		List<UUID> userIds = hierarchyService.getAllSubordinates(userId);
		List<UserPerformanceSnapshotDTO> list = repo.fetchSnapshot(userIds);
		list.forEach(dto -> {
			if (dto.getTotalSiteVisits() > 0) {
				BigDecimal ratio = BigDecimal.valueOf(dto.getTotalSales()).multiply(BigDecimal.valueOf(100))
						.divide(BigDecimal.valueOf(dto.getTotalSiteVisits()), 2, RoundingMode.HALF_UP);
				dto.setConversionRatio(ratio);
			} else {
				dto.setConversionRatio(BigDecimal.ZERO);
			}
		});

		return buildResponse(list);
	}
	
	public UserPerformanceDTO getUserPerformance(UUID userId, LocalDate fromDate, LocalDate toDate) {
        UserPerformanceDTO dto = new UserPerformanceDTO();
        var user = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        dto.setUserId(user.getUserId());
        dto.setUserName(user.getFullName());

        dto.setSiteVisits(repo.fetchSiteVisits(userId, fromDate, toDate));
        dto.setSales(repo.fetchSales(userId, fromDate, toDate));
        dto.setReceivable(repo.fetchReceivable(userId, fromDate, toDate));
        dto.setCommission(repo.fetchCommission(userId, fromDate, toDate));

        // Compute Visit-to-Sale conversion
        Set<String> visitedCustomers = dto.getSiteVisits().stream()
                .map(SiteVisitDetailDTO::getCustomerName)
                .collect(Collectors.toSet());

        long convertedCount = dto.getSales().stream()
                .filter(s -> visitedCustomers.contains(s.getCustomerName()))
                .count();

        dto.setVisitToSaleConversion(dto.getSiteVisits().size() > 0
                ? (double) convertedCount / dto.getSiteVisits().size() : 0);
        
        return dto;
    }

	public PerformanceResponse buildResponse(List<UserPerformanceSnapshotDTO> rows) {
		BigDecimal totalReceived = BigDecimal.ZERO;
		BigDecimal totalCommission = BigDecimal.ZERO;
		int totalVisits = 0;
		int totalSales = 0;

		for (UserPerformanceSnapshotDTO row : rows) {
			totalVisits += row.getTotalSiteVisits();
			totalSales += row.getTotalSales();
			totalReceived = totalReceived.add(row.getTotalReceived());
			totalCommission = totalCommission.add(row.getCommissionEarned());
		}
		return new PerformanceResponse(
				new UserPerformanceKpiDTO(totalVisits, totalSales, totalReceived, totalCommission), rows);
	}

	public UserPerformanceResponse getSnapshot(UUID loggedInUserId, UUID projectId, OffsetDateTime from, OffsetDateTime to,
			int page, int size) {
		List<UUID> userIds = hierarchyService.getAllSubordinates(loggedInUserId);
		userIds.add(loggedInUserId);
		UserPerformanceKpiDTO kpis = repo.fetchKpis(userIds, projectId, from, to);
		PagedResponse<UserPerformanceSnapshotDTO> snapshot = repo.fetchSnapshotPage(userIds, projectId, from, to, page, size);
		return new UserPerformanceResponse(kpis, snapshot);
	}

}
