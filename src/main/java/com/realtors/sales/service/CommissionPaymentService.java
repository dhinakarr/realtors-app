package com.realtors.sales.service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.realtors.common.util.AppUtil;
import com.realtors.sales.dto.CommissionRule;
import com.realtors.sales.dto.SaleContext;
import com.realtors.sales.dto.UserNode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CommissionPaymentService {
	private final JdbcTemplate jdbcTemplate;
	private static final Logger logger = LoggerFactory.getLogger(CommissionPaymentService.class);

	@Transactional
	public void distributeCommission(UUID saleId, BigDecimal basePrice) {
	    SaleContext sale = loadSaleContext(saleId);
	    List<UserNode> hierarchy = loadHierarchy(sale.sellerUserId());
	    List<CommissionRule> rules = loadCommissionRules(sale.projectId());

	    Map<UUID, BigDecimal> distribution = calculateDistribution(sale, hierarchy, rules);

	    Map<UUID, UUID> userRoleMap = hierarchy.stream()
	        .collect(Collectors.toMap(
	            UserNode::userId,
	            UserNode::roleId
	        ));
	    
	    persistCommissions(sale, basePrice, distribution, userRoleMap);
	}


	private SaleContext loadSaleContext(UUID saleId) {
		String sql = """
				    SELECT s.sale_id, s.project_id, s.sold_by, u.role_id AS seller_role_id, r.role_level  AS seller_role_level, r.finance_role AS seller_role_code, s.total_price
					FROM sales s
					JOIN app_users u ON u.user_id = s.sold_by
					JOIN roles r ON r.role_id = u.role_id
					WHERE s.sale_id = ?
				""";
		return jdbcTemplate.queryForObject(
		        sql, (rs, rowNum) -> new SaleContext(
		            rs.getObject("sale_id", UUID.class),
		            rs.getObject("project_id", UUID.class),
		            rs.getObject("sold_by", UUID.class),
		            rs.getObject("seller_role_id", UUID.class),
		            rs.getString("seller_role_code"),
		            rs.getInt("seller_role_level"),
		            rs.getBigDecimal("total_price")
		        ),
		        saleId
		    );
	}

	private List<UserNode> loadHierarchy(UUID sellerId) {
		String sql = """
				    WITH RECURSIVE chain AS (
				        SELECT u.user_id, u.manager_id, r.role_id, r.role_level
				        FROM app_users u
				        JOIN roles r ON r.role_id = u.role_id
				        WHERE u.user_id = ?
				        UNION ALL
				        SELECT m.user_id, m.manager_id, r.role_id, r.role_level
				        FROM app_users m
				        JOIN roles r ON r.role_id = m.role_id
				        JOIN chain c ON c.manager_id = m.user_id
				    )
				    SELECT * FROM chain
				""";
		return jdbcTemplate.query(sql, (rs, i) -> new UserNode(rs.getObject("user_id", UUID.class),
				rs.getObject("role_id", UUID.class), rs.getInt("role_level")), sellerId);
	}

	private List<CommissionRule> loadCommissionRules(UUID projectId) {

		String sql = """
				    SELECT cr.role_id, r.role_level, cr.percentage
				    FROM commission_rules cr
				    JOIN roles r ON r.role_id = cr.role_id
				    WHERE cr.project_id = ?
				""";

		return jdbcTemplate.query(sql, (rs, i) -> new CommissionRule(rs.getObject("role_id", UUID.class),
				rs.getInt("role_level"), rs.getBigDecimal("percentage")), projectId);
	}
	
	private boolean isHierarchyRoleLevel(int roleLevel) {
	    return roleLevel == 1 || roleLevel == 2 || roleLevel == 3;
	}
	
	private boolean isHierarchyRoleCode(String roleCode) {
	    return "PA".equals(roleCode)
	        || "PM".equals(roleCode)
	        || "PH".equals(roleCode);
	}
	
	private Map<UUID, BigDecimal> calculateDistribution(
	        SaleContext sale,
	        List<UserNode> hierarchy,
	        List<CommissionRule> rules
	) {
	    Map<UUID, BigDecimal> distribution = new LinkedHashMap<>();

	    UUID sellerId = sale.sellerUserId();
	    int sellerRoleLevel = sale.sellerRoleLevel();

	    // 1️⃣ Seller outside PA / PM / PH
	    if (!isHierarchyRoleCode(sale.sellerRoleCode())) {
	        distribution.put(sellerId, BigDecimal.valueOf(100));
	        return distribution;
	    }

	    // 2️⃣ roleLevel → userId
	    Map<Integer, UUID> levelToUser = hierarchy.stream()
	            .collect(Collectors.toMap(
	                    UserNode::roleLevel,
	                    UserNode::userId,
	                    (a, b) -> a   // safety
	            ));

	    // 3️⃣ Apply rules
	    for (CommissionRule rule : rules) {
	        int ruleRoleLevel = rule.roleLevel();

	        // PA sells → PA, PM, PH
	        if (sellerRoleLevel == 1 && ruleRoleLevel > 3) continue;

	        // PM sells → PM, PH
	        if (sellerRoleLevel == 2 && ruleRoleLevel < 2) continue;

	        // PH sells → PH only
	        if (sellerRoleLevel == 3 && ruleRoleLevel != 3) continue;

	        UUID receiverUserId = levelToUser.get(ruleRoleLevel);

	        // Skip missing hierarchy roles silently
	        if (receiverUserId == null) {
	            continue;
	        }

	        distribution.put(receiverUserId, rule.percentage());
	    }

	    // Safety net
	    if (distribution.isEmpty()) {
	        distribution.put(sellerId, BigDecimal.valueOf(100));
	    }

	    return distribution;
	}


	private void persistCommissions(SaleContext sale, BigDecimal basePrice, Map<UUID, BigDecimal> distribution, Map<UUID, UUID> userRoleMap) {
	    String sql = """
	        INSERT INTO sale_commissions (sale_id, user_id, role_id, percentage, commission_amount
	        ) VALUES (?, ?, ?, ?, ?)
	    """;

	    jdbcTemplate.batchUpdate(
	        sql,
	        distribution.entrySet(),
	        distribution.size(),
	        (ps, e) -> {
	        	UUID userId = e.getKey();
	            BigDecimal ratePerSqft = e.getValue();
	            ps.setObject(1, sale.saleId());
	            ps.setObject(2, userId);
	            BigDecimal commission = basePrice.multiply(AppUtil.nz(ratePerSqft));
	            // Seller earns under seller role, others under their own role
	            UUID roleId = userRoleMap.get(userId);
	            ps.setObject(3, roleId);
	            ps.setBigDecimal(4, ratePerSqft);
	            ps.setBigDecimal(5, commission);
	        }
	    );
	}
}
