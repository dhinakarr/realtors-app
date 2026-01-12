package com.realtors.dashboard.controller;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.realtors.dashboard.dto.PerformanceResponse;
import com.realtors.dashboard.dto.UserPerformanceDTO;
import com.realtors.dashboard.dto.UserPerformanceResponse;
import com.realtors.dashboard.dto.UserPrincipalDto;
import com.realtors.dashboard.service.UserPerformanceService;

@RestController
@RequestMapping("/api/performance")
public class UserPerformanceController {

	private UserPerformanceService performanceService;

	public UserPerformanceController(UserPerformanceService performanceService) {
		this.performanceService = performanceService;
	}

	@GetMapping("/users")
	public PerformanceResponse getUserPerformance(@AuthenticationPrincipal UserPrincipalDto principal,
			@RequestParam(required = false) UUID projectId, @RequestParam(required = false) LocalDate from,
			@RequestParam(required = false) LocalDate to) {
		return performanceService.getSnapshot(principal.getUserId());
	}

	@GetMapping("/users/paged")
	public UserPerformanceResponse getUserPagedPerformance(@AuthenticationPrincipal UserPrincipalDto principal,
			@RequestParam(required = false) UUID projectId, @RequestParam(required = false) LocalDate from,
			@RequestParam(required = false) LocalDate to, @RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size, @RequestParam(defaultValue = "full_name") String sortBy,
			@RequestParam(defaultValue = "asc") String sortDir) {
		int pageNum = 0;
		int pageSize =20; 
		OffsetDateTime fromTs = (from != null) ? from.atStartOfDay().atOffset(ZoneOffset.UTC) : null;
		OffsetDateTime toTs = (to != null) ? to.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC) : null;
		
		return performanceService.getSnapshot(principal.getUserId(), projectId, fromTs, toTs, pageNum, pageSize);
	}
	
	@GetMapping("/{userId}")
    public ResponseEntity<UserPerformanceDTO> getPerformance(
            @PathVariable UUID userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) {
        UserPerformanceDTO performance = performanceService.getUserPerformance(userId, fromDate, toDate);
        return ResponseEntity.ok(performance);
    }

}
