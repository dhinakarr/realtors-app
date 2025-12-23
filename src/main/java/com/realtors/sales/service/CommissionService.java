package com.realtors.sales.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.realtors.admin.dto.AppUserDto;
import com.realtors.admin.service.UserHierarchyService;
import com.realtors.common.util.AppUtil;
import com.realtors.sales.dto.CommissionRuleDTO;
import com.realtors.sales.dto.SaleCommissionDTO;
import com.realtors.sales.dto.SaleDTO;
import com.realtors.sales.finance.dto.CashFlowItemDTO;
import com.realtors.sales.finance.dto.CashFlowStatus;
import com.realtors.sales.finance.dto.PayableDetailsDTO;
import com.realtors.sales.repository.CommissionRuleRepository;
import com.realtors.sales.repository.SaleCommissionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CommissionService {

	private final UserHierarchyService hierarchyService;
	private final CommissionRuleRepository commissionRuleRepository;
	private final SaleCommissionRepository saleCommissionRepository;

	public void distributeCommission(SaleDTO sale) {
		// 1. Fetch salesperson
		UUID soldBy = sale.getSoldBy();
		// 2. Get hierarchy upwards (PA → PM → PH → MD ...)
		List<AppUserDto> hierarchy = hierarchyService.getHierarchyUpwards(soldBy);
		// 3. Get commission rules for project
		Map<UUID, CommissionRuleDTO> rules = commissionRuleRepository.getRulesByProject(sale.getProjectId());
		// 4. Distribute
		for (AppUserDto user : hierarchy) {
			CommissionRuleDTO rule = rules.get(user.getRoleId());
			if (rule == null)
				continue;

			BigDecimal pct = rule.getPercentage();
			BigDecimal amt = sale.getArea().multiply(AppUtil.nz(pct));

			saleCommissionRepository.insertCommission(
					new SaleCommissionDTO(sale.getSaleId(), user.getUserId(), user.getRoleId(), pct, amt));
		}
	}
	
	


	public SaleCommissionDTO insertCommission(SaleCommissionDTO dto) {
		return saleCommissionRepository.insertCommission(dto);
	}

	public List<SaleCommissionDTO> getCommissionsBySale(UUID saleId) {
		return saleCommissionRepository.findBySaleId(saleId);
	}

	public SaleCommissionDTO updateCommission(UUID commissionId, SaleCommissionDTO dto) {
		saleCommissionRepository.updateCommission(commissionId, dto.getPercentage(), dto.getCommissionAmount());
		return dto;
	}

	public List<SaleCommissionDTO> getCommissions(UUID saleId, UUID userId) {
		return saleCommissionRepository.findBySale(saleId, userId);
	}
	
	public BigDecimal getTotalPayable() {
		return saleCommissionRepository.getTotalPayable();
	}

	public BigDecimal getPaidThisMonth() {
		LocalDateTime from = LocalDate.now().withDayOfMonth(1).atStartOfDay();
		LocalDateTime to   = LocalDateTime.now();
		return saleCommissionRepository.getPaidBetween(from, to);
	}

	public List<CashFlowItemDTO> getPayables(
			LocalDate from,
			LocalDate to,
			CashFlowStatus status
	) {
		List<CashFlowItemDTO> list = saleCommissionRepository.getPayables(from, to);

		if (status == null) return list;

		return list.stream()
				.filter(i -> i.getStatus() == status)
				.toList();
	}
	
	public List<PayableDetailsDTO> getPayableDetails() {
		return saleCommissionRepository.getPayableDetails();
	}
}
