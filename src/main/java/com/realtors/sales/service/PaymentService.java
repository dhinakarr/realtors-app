package com.realtors.sales.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.realtors.common.util.AppUtil;
import com.realtors.sales.dto.PaymentDTO;
import com.realtors.sales.dto.SaleDTO;
import com.realtors.sales.repository.PaymentRepository;
import com.realtors.sales.repository.PaymentRepositoryImpl;
import com.realtors.sales.repository.SaleRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepositoryImpl paymentRepo;
    private final SaleRepository salestRepo;

    public PaymentDTO addPayment(PaymentDTO payment) {
        return paymentRepo.save(payment);
    }

    public List<PaymentDTO> getPaymentsBySale(UUID saleId) {
        return paymentRepo.findBySaleId(saleId);
    }
    
    public PaymentDTO save(PaymentDTO dto) {
    	UUID actorUserId = AppUtil.getCurrentUserId();
    	SaleDTO sale = salestRepo.findSaleByPlotId(dto.getPlotId());
    	dto.setSaleId(sale.getSaleId());
        if (dto.getPaymentDate() == null) {
            dto.setPaymentDate(LocalDateTime.now());
        }

        // RECEIVED payment → collected_by is required
        if (dto.getPaymentType().equals("RECEIVED")) {
            dto.setCollectedBy(actorUserId);
            dto.setPaidTo(null);
        }

        // PAID payment → paid_to is required
        if (dto.getPaymentType().equals("PAID")) {
            dto.setPaidTo(dto.getPaidTo());
            dto.setCollectedBy(null);
        }

        paymentRepo.save(dto);
        return dto;
    }

    public BigDecimal getOutstanding(UUID saleId) {
        BigDecimal totalSale = salestRepo.getTotalAmount(saleId);
        BigDecimal totalReceived = paymentRepo.getTotalReceived(saleId);
        return totalSale.subtract(totalReceived);
    }

    public BigDecimal getCommissionPaid(UUID saleId) {
        return paymentRepo.getTotalPaid(saleId);
    }
}
