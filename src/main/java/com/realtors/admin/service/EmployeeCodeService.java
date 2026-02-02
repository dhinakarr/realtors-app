package com.realtors.admin.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.realtors.admin.dto.EmployeeCode;
import com.realtors.admin.dto.HierarchyInfo;
import com.realtors.admin.dto.RoleInfo;
import com.realtors.admin.dto.RoleType;

@Service
public class EmployeeCodeService {

	private final JdbcTemplate jdbcTemplate;

	public EmployeeCodeService(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@Transactional("txManager")
	public EmployeeCode generateEmployeeCode(UUID roleId, UUID managerId, String branchCode) {
		RoleInfo role = getRole(roleId);
		HierarchyInfo hierarchyCode = resolveHierarchy(role.financeRole(), managerId, branchCode);
		int nextSeq = getNextSequence(branchCode, hierarchyCode.phGroupCode(), hierarchyCode.levelCode());
		String employeeId = buildEmployeeCode(branchCode, hierarchyCode.phGroupCode(), hierarchyCode.levelCode(),
				nextSeq);
		return new EmployeeCode(employeeId, hierarchyCode.phGroupCode(), nextSeq);
	}

	private RoleInfo getRole(UUID roleId) {
		String sql = """
				    SELECT finance_role, role_code, sub_role_code
				    FROM roles
				    WHERE role_id = ?
				""";
		return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> new RoleInfo(rs.getString("finance_role"),
				rs.getString("role_code"), rs.getString("sub_role_code")), roleId);
	}

	private String buildEmployeeCode(String branchCode, String roleCode, String subRoleCode, int seqNo) {
		return "D" + branchCode + roleCode + subRoleCode + String.format("%03d", seqNo);
	}

	private HierarchyInfo resolveHierarchy(String financeRole, UUID managerId, String branchCode) {

		// MD / HR / FINANCE → fixed root hierarchy
		if (RoleType.MD.name().equals(financeRole) || RoleType.HR.name().equals(financeRole)
				|| RoleType.FINANCE.name().equals(financeRole)) {

			return new HierarchyInfo("10", "01");
		}

		// PH → allocate NEW PH group
		if (RoleType.PH.name().equals(financeRole)) {

			jdbcTemplate.update("""
					    INSERT INTO ph_hierarchy_seq (branch_code, last_hierarchy)
					    VALUES (?, 10)
					    ON CONFLICT (branch_code) DO NOTHING
					""", branchCode);

			Integer phGroup = jdbcTemplate.queryForObject("""
					    UPDATE ph_hierarchy_seq
					    SET last_hierarchy = last_hierarchy + 1
					    WHERE branch_code = ?
					    RETURNING last_hierarchy
					""", Integer.class, branchCode);

			return new HierarchyInfo(String.format("%02d", phGroup), "01");
		}

		// PM / PA → inherit PH group, but own level
		if (managerId == null) {
			throw new IllegalStateException("Manager required for role " + financeRole);
		}
		
		List<String> result = jdbcTemplate.query(
			    """
			    WITH RECURSIVE chain AS (
			        SELECT u.user_id, u.manager_id, u.hierarchy_code, r.finance_role
			        FROM app_users u
			        JOIN roles r ON r.role_id = u.role_id
			        WHERE u.user_id = ?
			        UNION ALL
			        SELECT m.user_id, m.manager_id, m.hierarchy_code, r.finance_role
			        FROM app_users m
			        JOIN roles r ON r.role_id = m.role_id
			        JOIN chain c ON c.manager_id = m.user_id
			    )
			    SELECT hierarchy_code
			    FROM chain
			    WHERE finance_role = 'PH'
			    LIMIT 1
			    """,
			    (rs, rowNum) -> rs.getString(1),
			    managerId
			);

		String phGroup = result.isEmpty()
		        ? "10"   // 👈 DEFAULT PH GROUP
		        : result.get(0);

		String levelCode = switch (financeRole) {
			case "PM" -> "02";
			case "PA" -> "03";
			default -> "00";
		};
		return new HierarchyInfo(phGroup, levelCode);
	}

	private int getNextSequence(String branchCode, String phGroup, String levelCode) {
		String sql = """
				    SELECT COALESCE(MAX(seq_no), 0) + 1
				    FROM app_users
				    WHERE branch_code = ?
				      AND employee_id LIKE ?
				""";
		String likePattern = "D" + branchCode + phGroup + levelCode + "%";
		return jdbcTemplate.queryForObject(sql, Integer.class, branchCode, likePattern);
	}

}
