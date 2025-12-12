package com.realtors.sales.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.realtors.sales.dto.SaleCommissionDTO;
import com.realtors.sales.dto.SaleDTO;

public interface ReportsRepository {
	public List<SaleDTO> getSalesReport(LocalDate from, LocalDate to);
	public List<SaleCommissionDTO> getCommissionReport(UUID userId);
}
