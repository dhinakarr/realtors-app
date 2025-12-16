package com.realtors.sales.repository;

import java.util.UUID;
import java.math.BigDecimal;
import java.util.List;

import com.realtors.sales.dto.PaymentDTO;

public interface PaymentRepository {
    PaymentDTO insertPayment(PaymentDTO payment);
    List<PaymentDTO> findBySaleId(UUID saleId);
    BigDecimal getTotalReceived(UUID saleId);
}
