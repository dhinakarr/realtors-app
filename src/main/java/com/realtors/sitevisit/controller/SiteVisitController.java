package com.realtors.sitevisit.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.realtors.admin.dto.form.DynamicFormResponseDto;
import com.realtors.admin.dto.form.EditResponseDto;
import com.realtors.common.ApiResponse;
import com.realtors.common.util.AppUtil;
import com.realtors.dashboard.dto.UserPrincipalDto;
import com.realtors.dashboard.dto.UserRole;
import com.realtors.sitevisit.dto.PaymentPatchDTO;
import com.realtors.sitevisit.dto.SitePaymentDTO;
import com.realtors.sitevisit.dto.SiteVisitPatchDTO;
import com.realtors.sitevisit.dto.SiteVisitRequestDTO;
import com.realtors.sitevisit.dto.SiteVisitResponseDTO;
import com.realtors.sitevisit.service.SiteVisitService;
import com.realtors.sitevisit.service.SiteVisitServiceImpl;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/site-visits")
@RequiredArgsConstructor
public class SiteVisitController {

    private final SiteVisitServiceImpl siteVisitService;
    private final SiteVisitService visitService;
    

    private final Logger logger = LoggerFactory.getLogger(SiteVisitController.class);
    
    private boolean isCommonRole(UserPrincipalDto principal) {
    	Set<UserRole> role = principal.getRoles();
    	Set<UserRole> commonRole = Set.of(UserRole.FINANCE, UserRole.MD, UserRole.HR);
    	return role.stream().anyMatch(commonRole::contains);
    }
    
    @GetMapping("/form")
    public ResponseEntity<ApiResponse<DynamicFormResponseDto>> getVistForm(@AuthenticationPrincipal UserPrincipalDto principal) {
    	DynamicFormResponseDto form = visitService.getVisitFormData(AppUtil.isCommonRole(principal));
    	return ResponseEntity.ok(ApiResponse.success("Form details fetched", form, HttpStatus.OK));
    }
    
    @GetMapping("/form/{visitId}")
    public ResponseEntity<ApiResponse<EditResponseDto<SiteVisitResponseDTO>>> getVistEditForm(@AuthenticationPrincipal UserPrincipalDto principal, 
    											@PathVariable UUID visitId) {
    	EditResponseDto<SiteVisitResponseDTO> form = visitService.editEditFormResponse(visitId, AppUtil.isCommonRole(principal));
    	return ResponseEntity.ok(ApiResponse.success("Form details fetched", form, HttpStatus.OK));
    }

    /* =========================================================
       CREATE SITE VISIT
       ========================================================= */
    @PostMapping
    public ResponseEntity<ApiResponse<UUID>> createSiteVisit(
            @RequestBody SiteVisitRequestDTO dto) {
    	List<UUID> customerIds = dto.getCustomerId();
    	if (customerIds == null || customerIds.isEmpty()) {
    	    return ResponseEntity.badRequest().body(ApiResponse.success("Customer data is mandatoy", null));
    	}

        UUID siteVisitId = siteVisitService.createSiteVisit(dto);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Site visit created", siteVisitId));
    }

    /* =========================================================
       LIST SITE VISITS
       ========================================================= */
    @GetMapping
    public ResponseEntity<ApiResponse<List<SiteVisitResponseDTO>>> listSiteVisits(@AuthenticationPrincipal UserPrincipalDto principal,
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) UUID projectId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {

        List<SiteVisitResponseDTO> visits = siteVisitService.listSiteVisits(userId, projectId, fromDate, toDate, isCommonRole(principal));

        return ResponseEntity.ok(ApiResponse.success("Site visits retrieved", visits));
    }

    /* =========================================================
       GET SITE VISIT BY ID
       ========================================================= */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SiteVisitResponseDTO>> getSiteVisit(@AuthenticationPrincipal UserPrincipalDto principal,
            @PathVariable UUID id) {

        SiteVisitResponseDTO visit = siteVisitService.getSiteVisit(id, isCommonRole(principal));

        return ResponseEntity.ok(ApiResponse.success("Site visit retrieved", visit));
    }

    /* =========================================================
       PATCH SITE VISIT
       ========================================================= */
    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> patchSiteVisit(@AuthenticationPrincipal UserPrincipalDto principal,
            @PathVariable UUID id,
            @RequestBody SiteVisitPatchDTO dto) {

//        logger.info("Patching site visit {} with {}", id, dto);
        siteVisitService.patchSiteVisit(id, dto, isCommonRole(principal));

        return ResponseEntity.ok(ApiResponse.success("Site visit updated", null));
    }

    /* =========================================================
       DELETE SITE VISIT
       ========================================================= */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteSiteVisit(@PathVariable UUID id) {

        siteVisitService.deleteSiteVisit(id);

        return ResponseEntity.ok(ApiResponse.success("Site visit deleted", null));
    }

    /* =========================================================
       PAYMENTS (nested)
       ========================================================= */

    @PostMapping("/{id}/payments")
    public ResponseEntity<ApiResponse<Void>> addPayment(
            @PathVariable UUID id,
            @RequestBody SitePaymentDTO dto) {
    	
//        logger.info("Adding payment for site visit {}: {}", id, dto);
        siteVisitService.addPayment(id, dto);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Payment added", null));
    }

    @GetMapping("/{id}/payments")
    public ResponseEntity<ApiResponse<List<SitePaymentDTO>>> listPayments(
            @PathVariable UUID id) {
    	List<SitePaymentDTO> payments = siteVisitService.listPayments(id);
        return ResponseEntity.ok(ApiResponse.success("Payments retrieved", payments));
    }

    @PatchMapping("/{id}/payments/{paymentId}")
    public ResponseEntity<ApiResponse<Void>> patchPayment(
            @PathVariable UUID id,
            @PathVariable UUID paymentId,
            @RequestBody PaymentPatchDTO dto) {

//        logger.info("Patching payment {} for site visit {}: {}", paymentId, id, dto);
        siteVisitService.patchPayment(id, paymentId, dto);

        return ResponseEntity.ok(ApiResponse.success("Payment updated", null));
    }

    @DeleteMapping("/{id}/payments/{paymentId}")
    public ResponseEntity<ApiResponse<Void>> deletePayment(
            @PathVariable UUID id,
            @PathVariable UUID paymentId) {

        siteVisitService.deletePayments(id, paymentId);

        return ResponseEntity.ok(ApiResponse.success("Payment deleted", null));
    }
}
