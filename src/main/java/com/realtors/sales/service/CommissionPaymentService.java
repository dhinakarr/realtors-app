package com.realtors.sales.service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

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
//	private static final Logger logger = LoggerFactory.getLogger(CommissionPaymentService.class);

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
				    SELECT s.sale_id, s.project_id, s.sold_by, u.role_id AS seller_role_id, r.role_level   AS seller_role_level, s.total_price
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
	
	private Map<UUID, BigDecimal> calculateDistribution(SaleContext sale, List<UserNode> hierarchy, List<CommissionRule> rules) {

	    Map<UUID, UUID> roleToUser = hierarchy.stream()
	        .collect(Collectors.toMap(
	            UserNode::roleId,
	            UserNode::userId
	        ));

	    Map<UUID, BigDecimal> distribution = new HashMap<>();
	    for (CommissionRule rule : rules) {
	        UUID receiverUserId;
	        if (rule.roleLevel() > sale.sellerRoleLevel()) {
	            // Lower-level role → accumulates to seller
	            receiverUserId = sale.sellerUserId();
	        } else {
	            // Same or higher role → actual role holder
	            receiverUserId = roleToUser.get(rule.roleId());
	        }
	        distribution.merge(receiverUserId, rule.percentage(), BigDecimal::add);
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
	            BigDecimal percentage = e.getValue();
	            ps.setObject(1, sale.saleId());
	            ps.setObject(2, userId);
	            BigDecimal commission = basePrice.multiply(AppUtil.nz(percentage));
	            // Seller earns under seller role, others under their own role
	            UUID roleId = userId.equals(sale.sellerUserId())
	                    ? sale.sellerRoleId()
	                    : userRoleMap.get(userId);

	            ps.setObject(3, roleId);
	            ps.setBigDecimal(4, percentage);
	            ps.setBigDecimal(5, commission);
	        }
	    );
	}
}
