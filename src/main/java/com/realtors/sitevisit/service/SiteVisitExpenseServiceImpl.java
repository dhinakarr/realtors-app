package com.realtors.sitevisit.service;

import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.realtors.sitevisit.dto.ExpenseDTO;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SiteVisitExpenseServiceImpl implements SiteVisitExpenseService {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void saveExpenses(UUID siteVisitId, List<ExpenseDTO> expenses) {

        if (expenses == null) return;

        for (ExpenseDTO e : expenses) {
            jdbcTemplate.update("""
                INSERT INTO site_visit_expenses
                (expense_id, site_visit_id, expense_type_id,
                 amount, paid_by, expense_date, bill_reference, remarks)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """,
                UUID.randomUUID(),
                siteVisitId,
                e.getExpenseTypeId(),
                e.getAmount(),
                e.getPaidBy(),
                e.getExpenseDate(),
                e.getBillReference(),
                e.getRemarks()
            );
        }
    }
}
