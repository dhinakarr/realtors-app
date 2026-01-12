package com.realtors.sales.test;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

import com.realtors.sales.dto.PaymentDTO;
import com.realtors.sales.dto.SaleCommissionDTO;
import com.realtors.sales.dto.SaleCreateRequest;
import com.realtors.sales.dto.SaleDTO;
import com.realtors.sales.service.CommissionService;
import com.realtors.sales.service.PaymentService;
import com.realtors.sales.service.SaleService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@SpringBootApplication(scanBasePackages = "com.yourapp")
public class SalesModuleTest {

    public static void main(String[] args) {
        ApplicationContext context = SpringApplication.run(SalesModuleTest.class, args);

        // --- Get services ---
        SaleService saleService = context.getBean(SaleService.class);
        PaymentService paymentService = context.getBean(PaymentService.class);
        CommissionService commissionService = context.getBean(CommissionService.class);

        // --- Sample Data ---
        UUID plotId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID projectId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        UUID customerId = UUID.fromString("33333333-3333-3333-3333-333333333333");
        UUID soldByPA = UUID.fromString("44444444-4444-4444-4444-444444444444"); // PA user
        UUID stageBooking = UUID.fromString("55555555-5555-5555-5555-555555555555");
        UUID stage1 = UUID.fromString("66666666-6666-6666-6666-666666666666");

        // Sample Project Charges
        BigDecimal regCharges = new BigDecimal("5000");
        BigDecimal docCharges = new BigDecimal("3000");
        BigDecimal otherCharges = new BigDecimal("2000");
        BigDecimal extraCharges = regCharges.add(docCharges).add(otherCharges);

        // --- 1. Create Sale ---
        SaleCreateRequest saleRequest = new SaleCreateRequest();
        saleRequest.setPlotId(plotId);
        saleRequest.setCustomerId(customerId);
        saleRequest.setSoldBy(soldByPA);
        saleRequest.setExtraCharges(extraCharges);
        saleRequest.setAdvanceAmount(new BigDecimal("50000"));

        SaleDTO sale = saleService.createSale(saleRequest);
        System.out.println("Sale Created:\n" + sale);

        // --- 2. Record Payments ---

        // Payment 1: Booking Advance
        PaymentDTO payment1 = new PaymentDTO();
        payment1.setSaleId(sale.getSaleId());
//        payment1.setStageId(stageBooking);
        payment1.setAmount(new BigDecimal("50000"));
        payment1.setPaymentMode("ONLINE");
        payment1.setTransactionRef("TXN123456");
        payment1.setRemarks("Booking advance");
        payment1.setPaymentDate(LocalDate.now());
        paymentService.addPayment(payment1);

        // Payment 2: Stage 1
        PaymentDTO payment2 = new PaymentDTO();
        payment2.setSaleId(sale.getSaleId());
//        payment2.setStageId(stage1);
        payment2.setAmount(new BigDecimal("100000"));
        payment2.setPaymentMode("CHEQUE");
        payment2.setTransactionRef("CHQ7890");
        payment2.setRemarks("Stage 1 payment");
        payment2.setPaymentDate(LocalDate.now().plusDays(10));
        paymentService.addPayment(payment2);

        System.out.println("Payments Recorded");

        // --- 3. Query Sale ---
        SaleDTO saleDetails = saleService.getSaleById(sale.getSaleId());
        System.out.println("\nSale Details:\n" + saleDetails);

        // --- 4. Query Payments ---
        List<PaymentDTO> payments = paymentService.getPaymentsBySale(sale.getSaleId());
        System.out.println("\nPayments:");
        payments.forEach(System.out::println);

        // --- 5. Query Commission Distribution ---
        List<SaleCommissionDTO> commissions = commissionService.getCommissionsBySale(sale.getSaleId());
        System.out.println("\nCommission Distribution:");
        commissions.forEach(System.out::println);

        // --- 6. Confirm Sale (Optional) ---
        saleService.confirmSale(sale.getSaleId());
        System.out.println("\nSale Confirmed Successfully");
    }
}
