package com.realtors.sitevisit.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.realtors.common.util.AppUtil;
import com.realtors.customers.dto.CustomerMiniDto;
import com.realtors.sitevisit.dto.AgentDto;
import com.realtors.sitevisit.dto.ExpenseDTO;
import com.realtors.sitevisit.dto.PageResponse;
import com.realtors.sitevisit.dto.PaymentPatchDTO;
import com.realtors.sitevisit.dto.PaymentSummaryDTO;
import com.realtors.sitevisit.dto.ProjectMiniDto;
import com.realtors.sitevisit.dto.SitePaymentDTO;
import com.realtors.sitevisit.dto.SiteVisitFormDataDto;
import com.realtors.sitevisit.dto.SiteVisitPatchDTO;
import com.realtors.sitevisit.dto.SiteVisitPaymentResponseDTO;
import com.realtors.sitevisit.dto.SiteVisitRequestDTO;
import com.realtors.sitevisit.dto.SiteVisitResponseDTO;
import com.realtors.sitevisit.dto.VehicleUsageDTO;
import com.realtors.sitevisit.mapper.AgentRowMapper;
import com.realtors.sitevisit.mapper.CustomerRowMapper;
import com.realtors.sitevisit.mapper.SitePaymentRowMapper;
import com.realtors.sitevisit.mapper.SiteProjectRowMapper;
import com.realtors.sitevisit.mapper.SiteVisitRowMapper;
import com.realtors.sitevisit.repository.SiteVisitAccountRepository;
import com.realtors.sitevisit.repository.SiteVisitCustomerRepository;
import com.realtors.sitevisit.repository.SiteVisitPaymentRepository;
import com.realtors.sitevisit.repository.SiteVisitRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SiteVisitServiceImpl {

	private final JdbcTemplate jdbcTemplate;
	private final SiteVisitRepository visitRepo;
	private final SiteVisitCustomerRepository customerRepo;
	private final SiteVisitAccountRepository accountRepo;
	private final SiteVisitPaymentRepository paymentRepo;

	@Transactional
	public UUID createSiteVisit(SiteVisitRequestDTO dto) {
		UUID siteVisitId = visitRepo.create(dto);
		customerRepo.insert(siteVisitId, dto.getCustomerId());
		accountRepo.create(siteVisitId, dto.getExpenseAmount());
		return siteVisitId;
	}

	@Transactional
	public void addPayment(UUID siteVisitId, SitePaymentDTO dto) {
		BigDecimal existingBallance = AppUtil.nz(accountRepo.getBalanceAmount(siteVisitId));
		BigDecimal inputAmount = AppUtil.nz(dto.getAmount());
		
		if (inputAmount.compareTo(existingBallance) > 0) {
			throw new IllegalArgumentException("Payment amount cannot exceed balance");
		}
		// 1. Insert payment
		paymentRepo.insert(siteVisitId, dto);

		// 2. Recalculate totals
		BigDecimal expense = visitRepo.getExpenseAmount(siteVisitId);
		BigDecimal totalPaid = paymentRepo.getTotalPaid(siteVisitId);
		BigDecimal balance = expense.subtract(AppUtil.nz(totalPaid));

		String status = balance.signum() <= 0 ? "CLOSED" : "OPEN";
		// 3. Update account
		accountRepo.updateTotals(siteVisitId, totalPaid, balance, status);
	}

	public SiteVisitFormDataDto getFormData() {
		SiteVisitFormDataDto dto = new SiteVisitFormDataDto();
		List<AgentDto> agents = getAgentList();
		List<CustomerMiniDto> customers = getCustomerList();
		List<ProjectMiniDto> projects = getProjectList();

		dto.setAgents(agents);
		dto.setCustomers(customers);
		dto.setProjects(projects);

		return dto;
	}

    @Transactional(readOnly = true)
    public SiteVisitResponseDTO getSiteVisit(UUID siteVisitId) {
        SiteVisitResponseDTO visit = visitRepo.findById(siteVisitId);
        if(visit == null) {
              new IllegalArgumentException("Site visit not found");
              return null;
        }
        visit.setCustomers(customerRepo.findCustomers(siteVisitId));
        return visit;
    }
    
    @Transactional(readOnly = true)
    public List<SiteVisitResponseDTO> listSiteVisits(UUID userId, UUID projectId, LocalDate fromDate, LocalDate toDate) {
        List<SiteVisitResponseDTO> visits = visitRepo.list(userId, projectId, fromDate, toDate);
        return visits;
    }
    
	private void recalcAccount(UUID siteVisitId) {
	    BigDecimal expense = visitRepo.getExpenseAmount(siteVisitId);
	    BigDecimal totalPaid = paymentRepo.getTotalPaid(siteVisitId);
	    BigDecimal balance = expense.subtract(totalPaid);
	    String status = balance.signum() <= 0 ? "CLOSED" : "OPEN";

	    accountRepo.updateTotals(siteVisitId, totalPaid, balance, status);
	}

	@Transactional
	public void patchSiteVisit(UUID siteVisitId, SiteVisitPatchDTO dto) {
		SiteVisitResponseDTO existing = visitRepo.findById(siteVisitId);
	    BigDecimal oldExpense = existing.getExpenseAmount();
	    visitRepo.patch(siteVisitId, dto);

	    if (dto.getCustomerIds() != null) {
	        customerRepo.replace(siteVisitId, dto.getCustomerIds());
	    }
	    // If expense changed → recalc account
	    if (dto.getExpenseAmount() != null &&  dto.getExpenseAmount().compareTo(oldExpense) != 0) {
	        recalcAccount(siteVisitId);
	    }
	}

	@Transactional
	public void patchPayment(UUID siteVisitId,  UUID paymentId, PaymentPatchDTO dto) {
	    paymentRepo.patch(paymentId, dto);
	    // Always recalc (amount might change)
	    recalcAccount(siteVisitId);
	}

	@Transactional(readOnly = true)
    public List<SitePaymentDTO> listPayments(UUID siteVisitId) {
        return paymentRepo.listBySiteVisit(siteVisitId);
    }
	
	public void deleteSiteVisit(UUID siteVisitId) {
        BigDecimal totalPaid = paymentRepo.getTotalPaid(siteVisitId);
        if (totalPaid.signum() > 0) {
            throw new IllegalStateException(
                "Cannot delete visit with payments");
        }
        visitRepo.delete(siteVisitId);
    }

	@Transactional
	public void submitForApproval(UUID siteVisitId) {
		String status = getCurrentStatus(siteVisitId);
		if (!List.of("DRAFT", "REJECTED").contains(status)) {
			throw new IllegalStateException("Only DRAFT or REJECTED visits can be submitted");
		}
		jdbcTemplate.update("""
				    UPDATE site_visit_accounts
				    SET status = 'SUBMITTED'
				    WHERE site_visit_id = ?::uuid
				""", siteVisitId);
	}

	@Transactional
	public void rejectSiteVisit(UUID siteVisitId, String reason) {
		String status = getCurrentStatus(siteVisitId);
		if (!"SUBMITTED".equals(status)) {
			throw new IllegalStateException("Only SUBMITTED visits can be rejected");
		}

		jdbcTemplate.update("""
				    UPDATE site_visit_accounts
				    SET status = 'REJECTED'
				    WHERE site_visit_id = ?::uuid
				""", siteVisitId);
	}

	@Transactional
	public void closeSiteVisit(UUID siteVisitId) {
		String status = getCurrentStatus(siteVisitId);
		if (!"APPROVED".equals(status)) {
			throw new IllegalStateException("Only APPROVED visits can be marked as PAID");
		}
		jdbcTemplate.update("""
				    UPDATE site_visit_accounts
				    SET status = 'PAID'
				    WHERE site_visit_id = ?::uuid
				""", siteVisitId);
	}

	@Transactional
	public void updateExpenses(UUID siteVisitId, List<ExpenseDTO> expenses) {
		validateEditable(siteVisitId);
		jdbcTemplate.update("""
				    DELETE FROM site_visit_expenses WHERE site_visit_id = ?::uuid
				""", siteVisitId);

		if (expenses == null)
			return;

		for (ExpenseDTO e : expenses) {
			jdbcTemplate.update("""
					    INSERT INTO site_visit_expenses
					    (expense_id, site_visit_id, expense_type_id,
					     amount, paid_by, expense_date, bill_reference, remarks)
					    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
					""", UUID.randomUUID(), siteVisitId, e.getExpenseTypeId(), e.getAmount(), e.getPaidBy(),
					e.getExpenseDate(), e.getBillReference(), e.getRemarks());
		}
	}

	@Transactional
	public void updatePayments(UUID siteVisitId, List<SitePaymentDTO> payments) {
		// validateEditable(siteVisitId);
		jdbcTemplate.update("""
				    DELETE FROM site_visit_payments WHERE site_visit_id = ?::uuid
				""", siteVisitId);

		if (payments == null)
			return;

		for (SitePaymentDTO p : payments) {
			jdbcTemplate.update("""
					    INSERT INTO site_visit_payments
					    (payment_id, site_visit_id, user_id,
					     amount, payment_mode, payment_date, remarks)
					    VALUES (?, ?, ?, ?, ?, ?, ?)
					""", UUID.randomUUID(), siteVisitId, p.getUserId(), p.getAmount(), p.getPaymentMode(),
					p.getPaymentDate(), p.getRemarks());
		}
	}

	@Transactional
	public void updateVehicle(UUID siteVisitId, VehicleUsageDTO vehicle) {
		validateEditable(siteVisitId);
		jdbcTemplate.update("""
				    DELETE FROM site_visit_vehicles WHERE site_visit_id = ?::uuid
				""", siteVisitId);

		if (vehicle == null)
			return;

		jdbcTemplate.update("""
				    INSERT INTO site_visit_vehicles
				    (site_visit_id, vehicle_id, fuel_cost, driver_cost, toll_cost, rent_cost)
				    VALUES (?, ?, ?, ?, ?, ?)
				""", siteVisitId, vehicle.getVehicleId(), vehicle.getFuelCost(), vehicle.getDriverCost(),
				vehicle.getTollCost(), vehicle.getRentCost());
	}

	public void updateCustomers(UUID siteVisitId, List<UUID> customerIds) {
		// TODO Auto-generated method stub
	}

	private String getCurrentStatus(UUID siteVisitId) {
		return jdbcTemplate.queryForObject("""
				    SELECT status
				    FROM site_visit_accounts
				    WHERE site_visit_id = ?::uuid
				""", String.class, siteVisitId);
	}

	private void validateEditable(UUID siteVisitId) {
		String status = getCurrentStatus(siteVisitId);
		if (!List.of("DRAFT", "REJECTED").contains(status)) {
			throw new IllegalStateException("Site visit cannot be modified in status: " + status);
		}
	}

	private void validateDeletable(UUID siteVisitId) {
		String status = getCurrentStatus(siteVisitId);
		if (!"DRAFT".equals(status)) {
			throw new IllegalStateException("Only DRAFT site visits can be deleted");
		}
	}

	public PageResponse<SiteVisitResponseDTO> listSiteVisitsPages(UUID agentId, UUID projectId, String status,
			LocalDate fromDate, LocalDate toDate, int page, int size) {
		FilterQuery fq = buildFilter(agentId, projectId, status, fromDate, toDate);
		long total = countSiteVisits(fq);
		int offset = page * size;
		String dataSql = """
				    SELECT
				        sv.site_visit_id,
				        sv.visit_date,
				        sv.user_id ,
				        a.full_name AS user_name,
				        sv.project_id,
				        p.project_name,
				        acc.total_expense,
				        acc.total_paid,
				        acc.balance,
				        acc.status
				    FROM site_visits sv
				    JOIN site_visit_accounts acc
				      ON acc.site_visit_id = sv.site_visit_id
				    JOIN app_users a
				      ON a.user_id = sv.user_id
				    JOIN projects p
				      ON p.project_id = sv.project_id
				""" + fq.whereSql + """
				    ORDER BY sv.visit_date DESC
				    LIMIT ? OFFSET ?
				""";

		List<Object> params = new ArrayList<>(fq.params);
		params.add(size);
		params.add(offset);
		List<SiteVisitResponseDTO> content = jdbcTemplate.query(dataSql, params.toArray(), new SiteVisitRowMapper());
		return new PageResponse<>(content, total, page, size);
	}

	private static class FilterQuery {
		String whereSql;
		List<Object> params;
	}

	private FilterQuery buildFilter(UUID agentId, UUID projectId, String status, LocalDate fromDate, LocalDate toDate) {
		StringBuilder where = new StringBuilder(" WHERE 1=1 ");
		List<Object> params = new ArrayList<>();

		if (agentId != null) {
			where.append(" AND sv.user_id = ? ");
			params.add(agentId);
		}
		if (projectId != null) {
			where.append(" AND sv.project_id = ? ");
			params.add(projectId);
		}
		if (status != null && !status.isBlank()) {
			where.append(" AND acc.status = ? ");
			params.add(status);
		}
		if (fromDate != null) {
			where.append(" AND sv.visit_date >= ? ");
			params.add(fromDate);
		}
		if (toDate != null) {
			where.append(" AND sv.visit_date <= ? ");
			params.add(toDate);
		}

		FilterQuery fq = new FilterQuery();
		fq.whereSql = where.toString();
		fq.params = params;
		return fq;
	}

	private long countSiteVisits(FilterQuery fq) {
		String countSql = """
				    SELECT COUNT(*)
				    FROM site_visits sv
				    JOIN site_visit_accounts acc
				      ON acc.site_visit_id = sv.site_visit_id
				""" + fq.whereSql;

		return jdbcTemplate.queryForObject(countSql, fq.params.toArray(), Long.class);
	}

	private List<CustomerMiniDto> getCustomerList() {
		return jdbcTemplate.query("""
				    SELECT c.customer_id, c.customer_name
				    FROM customers c
				""", new CustomerRowMapper());
	}

	private List<AgentDto> getAgentList() {
		return jdbcTemplate.query("""
				    SELECT user_id, full_name
				    FROM app_users
				""", new AgentRowMapper());
	}

	private List<ProjectMiniDto> getProjectList() {
		return jdbcTemplate.query("""
				    SELECT project_id, project_name
				    FROM projects
				""", new SiteProjectRowMapper());
	}

	public SiteVisitPaymentResponseDTO getPayments(UUID visitId) {
		SiteVisitPaymentResponseDTO retDto = new SiteVisitPaymentResponseDTO();
		String sql = "select * from site_visit_payments where site_visit_id=?::uuid";
		List<SitePaymentDTO> payments = jdbcTemplate.query(sql, new SitePaymentRowMapper(), visitId);

		// 2. Summary (aggregated query)
		String summarySql = "SELECT COALESCE(SUM(amount), 0) AS total_paid  FROM site_visit_payments  WHERE site_visit_id = ?::uuid";
		BigDecimal totalPaid = jdbcTemplate.queryForObject(summarySql, BigDecimal.class, visitId);

		// 3. Total expense from site_visit_accounts / expenses
		String expenseSql = "SELECT COALESCE(SUM(amount), 0)  FROM site_visit_expenses WHERE site_visit_id = ?::uuid";
		BigDecimal totalExpense = jdbcTemplate.queryForObject(expenseSql, BigDecimal.class, visitId);

		PaymentSummaryDTO summary = new PaymentSummaryDTO();
		summary.setTotalExpense(totalExpense);
		summary.setTotalPaid(totalPaid);
		summary.setBalanceAmount(totalExpense.subtract(totalPaid));

		retDto.setSummary(summary);
		retDto.setPayments(payments);
		return retDto;
	}

	public SiteVisitPaymentResponseDTO addPayments(UUID siteVisitId, SitePaymentDTO payments) {
		if (payments == null)
			return null;

		jdbcTemplate.update("""
				    INSERT INTO site_visit_payments
				    (payment_id, site_visit_id, user_id,
				     amount, payment_mode, payment_date, remarks)
				    VALUES (?, ?, ?, ?, ?, ?, ?)
				""", UUID.randomUUID(), siteVisitId, payments.getUserId(), payments.getAmount(),
				payments.getPaymentMode(), OffsetDateTime.now(), payments.getRemarks());
		SiteVisitPaymentResponseDTO paymentsDto = getPayments(siteVisitId);
		return paymentsDto;
	}

	private Map<UUID, List<CustomerMiniDto>> fetchCustomers(List<UUID> siteVisitIds) {
		if (siteVisitIds == null || siteVisitIds.isEmpty()) {
			return Map.of();
		}

		String placeholders = siteVisitIds.stream().map(id -> "?").collect(Collectors.joining(","));
		String sql = """
				SELECT  svc.site_visit_id,  c.customer_id, c.customer_name, c.mobile
				FROM site_visit_customers svc
				JOIN customers c
				  ON c.customer_id = svc.customer_id
				WHERE svc.site_visit_id IN (""" + placeholders + ")";

		return jdbcTemplate.query(sql, siteVisitIds.toArray(), rs -> {
			Map<UUID, List<CustomerMiniDto>> map = new HashMap<>();
			while (rs.next()) {
				UUID siteVisitId = rs.getObject("site_visit_id", UUID.class);
				map.computeIfAbsent(siteVisitId, k -> new ArrayList<>())
						.add(new CustomerMiniDto(rs.getObject("customer_id", UUID.class), rs.getString("customer_name"),
								rs.getObject("mobile", Long.class), null// ✅ safe
				));
			}
			return map;
		});
	}
	public void deletePayments(UUID visitId, UUID paymentId) {
		paymentRepo.delete(paymentId, visitId);
	}
}
