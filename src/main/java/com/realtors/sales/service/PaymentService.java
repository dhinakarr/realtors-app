package com.realtors.sales.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.realtors.sales.dto.PaymentDTO;
import com.realtors.sales.repository.PaymentRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;

    public PaymentDTO addPayment(PaymentDTO payment) {
        return paymentRepository.insertPayment(payment);
    }

    public List<PaymentDTO> getPaymentsBySale(UUID saleId) {
        return paymentRepository.findBySaleId(saleId);
    }
}
