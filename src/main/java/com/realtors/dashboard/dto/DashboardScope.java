package com.realtors.dashboard.dto;

import java.util.UUID;
import java.time.LocalDate;
import java.util.Set;
import lombok.Builder;
import lombok.Data;


@Data
@Builder
public class DashboardScope {

    private boolean all;              // MD
    private UUID userId;               // PA
    private Set<UUID> projectIds;     // PM / PH
    private boolean financeOnly;       // FINANCE
    private boolean hrOnly;            // HR
    private Set<UUID> userIds;
    private UUID customerId;
    private LocalDate fromDate;
    private LocalDate toDate;

    public static DashboardScope all() {
        return DashboardScope.builder().all(true).build();
    }
    
    public boolean isCustomer() {
        return customerId != null;
    }
    
    public boolean hasDateRange() {
        return fromDate != null && toDate != null;
    }

    public static DashboardScope user(UUID userId) {
        return DashboardScope.builder().userId(userId).build();
    }
    
    public boolean hasProjectScope() {
        return projectIds != null && !projectIds.isEmpty();
    }
    
    public static DashboardScope empty() {
        return DashboardScope.builder().build();
    }

    public static DashboardScope projects(Set<UUID> projectIds) {
        return DashboardScope.builder().projectIds(projectIds).build();
    }

    public static DashboardScope finance() {
        return DashboardScope.builder().financeOnly(true).build();
    }

    public static DashboardScope hr() {
        return DashboardScope.builder().hrOnly(true).build();
    }

    public static DashboardScope none() {
        return DashboardScope.builder().build();
    }
}
