package com.realtors.sitevisit.service;

import java.util.UUID;
import java.util.List;
import com.realtors.sitevisit.dto.ExpenseDTO;

public interface SiteVisitExpenseService {
    void saveExpenses(UUID siteVisitId, List<ExpenseDTO> expenses);
}