package com.realtors.sales.service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.realtors.admin.dto.AppUserDto;
import com.realtors.admin.dto.RoleDto;
import com.realtors.admin.service.RoleService;
import com.realtors.admin.service.UserService;
import com.realtors.sales.dto.CommissionSpreadRuleDTO;
import com.realtors.sales.dto.SaleCommissionDTO;
import com.realtors.sales.repository.PaymentRuleRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class CommissionDistributionService {

	private final CommissionPaymentService commissionService;
	private final CommissionCalculator calculator;
	private final PaymentRuleRepository paymentRuleRepository;
	private final UserService userService;
	private final RoleService roleService;
	private static final Logger logger = LoggerFactory.getLogger(CommissionDistributionService.class);

	@Transactional("txManager")
	public List<SaleCommissionDTO> distributeCommission(UUID projectId, UUID soldByUserId, BigDecimal saleAmount,
			BigDecimal sqft, UUID saleId) {
		logger.info("@CommissionDistributionService.distributeCommission saleId: {}", saleId);
		DistributionContext ctx = buildContext(projectId, soldByUserId, saleAmount, sqft, saleId);
		for (CommissionSpreadRuleDTO rule : ctx.rules) {
			processRule(ctx, rule);
		}
		applyAbsorbedToSeller(ctx);
		List<SaleCommissionDTO> result = buildResult(ctx);
		commissionService.saveAll(result);
		return result;
	}

	// ==============================
	// Core Rule Processor
	// ==============================

	private void processRule(DistributionContext ctx, CommissionSpreadRuleDTO rule) {
		RoleDto ruleRole = roleService.getRoleById(rule.getRoleId()).orElseThrow();
		int ruleLevel = ruleRole.getRoleLevel();
		int sellerLevel = ctx.sellerLevel;

		BigDecimal pct = rule.getCommissionValue();
		BigDecimal amt = calculator.calculate(rule, ctx.saleAmount, ctx.sqft);

		// 1️⃣ Fixed-user rules (MD / OWNER)
		if (rule.getUserId() != null) {
			add(ctx, rule.getUserId(), rule.getRoleId(), pct, amt);
			return;
		}

		// 2️⃣ Seller role rule
		if (rule.getRoleId().equals(ctx.seller.getRoleId())) {
			add(ctx, ctx.seller.getUserId(), ctx.seller.getRoleId(), pct, amt);
			return;
		}

		// 3️⃣ Junior roles (ruleLevel > sellerLevel) → always absorbed
		if (ruleLevel > sellerLevel) {
			absorb(ctx, pct, amt);
			return;
		}

		// 4️⃣ Senior roles (ruleLevel < sellerLevel)
		AppUserDto senior = ctx.hierarchyUpwards.stream()
		        .map(userService::findById)          // UUID → Optional<AppUserDto>
		        .flatMap(Optional::stream)           // unwrap Optional
		        .filter(u -> u.getRoleId().equals(rule.getRoleId()))
		        .findFirst()
		        .orElse(null);

		if (senior != null) {
			add(ctx, senior.getUserId(), senior.getRoleId(), pct, amt);
		} else {
			absorb(ctx, pct, amt);
		}
	}
	// ==============================
	// Context Builder
	// ==============================
	private DistributionContext buildContext(UUID projectId, UUID soldByUserId, BigDecimal saleAmount, BigDecimal sqft,
			UUID saleId) {
		AppUserDto seller = userService.findById(soldByUserId)
				.orElseThrow(() -> new RuntimeException("Seller not found"));

		RoleDto sellerRole = roleService.getRoleById(seller.getRoleId())
				.orElseThrow(() -> new RuntimeException("Role not found"));

		DistributionContext ctx = new DistributionContext();
		ctx.saleId = saleId;
		ctx.seller = seller;
		ctx.sellerLevel = sellerRole.getRoleLevel();
		ctx.saleAmount = saleAmount;
		ctx.sqft = sqft;
		ctx.rules = paymentRuleRepository.findActiveRulesByProject(projectId);
		ctx.hierarchyUpwards = userService.getHierarchyUpwards(seller.getUserId());
		return ctx;
	}

	// ==============================
	// Absorption & Addition
	// ==============================
	private void absorb(DistributionContext ctx, BigDecimal pct, BigDecimal amt) {
		ctx.absorbedPct = ctx.absorbedPct.add(pct);
		ctx.absorbedAmt = ctx.absorbedAmt.add(amt);
	}

	private void applyAbsorbedToSeller(DistributionContext ctx) {
		if (ctx.absorbedPct.compareTo(BigDecimal.ZERO) <= 0)
			return;

		add(ctx, ctx.seller.getUserId(), ctx.seller.getRoleId(), ctx.absorbedPct, ctx.absorbedAmt);
	}

	private void add(DistributionContext ctx, UUID userId, UUID roleId, BigDecimal pct, BigDecimal amt) {
		ctx.pctMap.merge(userId, pct, BigDecimal::add);
		ctx.amtMap.merge(userId, amt, BigDecimal::add);
		ctx.roleMap.put(userId, roleId);
	}

	// ==============================
	// Result Builder
	// ==============================
	private List<SaleCommissionDTO> buildResult(DistributionContext ctx) {
		return ctx.amtMap.keySet().stream().map(uid -> new SaleCommissionDTO(ctx.saleId, uid, ctx.roleMap.get(uid),
				ctx.pctMap.get(uid), ctx.amtMap.get(uid))).toList();
	}

	// ==============================
	// Internal Context
	// ==============================
	private static class DistributionContext {
		UUID saleId;
		AppUserDto seller;
		int sellerLevel;

		BigDecimal saleAmount;
		BigDecimal sqft;

		List<CommissionSpreadRuleDTO> rules;
		List<UUID> hierarchyUpwards;

		BigDecimal absorbedPct = BigDecimal.ZERO;
		BigDecimal absorbedAmt = BigDecimal.ZERO;

		Map<UUID, BigDecimal> pctMap = new HashMap<>();
		Map<UUID, BigDecimal> amtMap = new HashMap<>();
		Map<UUID, UUID> roleMap = new HashMap<>();
	}
}
