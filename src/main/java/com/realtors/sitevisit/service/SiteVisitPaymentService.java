package com.realtors.sitevisit.service;

import java.util.UUID;
import java.util.List;
import com.realtors.sitevisit.dto.SitePaymentDTO;

public interface SiteVisitPaymentService {
    void savePayments(UUID siteVisitId, List<SitePaymentDTO> payments);
}