package com.realtors.dashboard.repository;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class DashboardRepository {

	private final JdbcTemplate jdbc;

    public DashboardRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }
    
    /* Total Leads Created */
    public Long countLeadsCreated(UUID userId) {
        String sql = "SELECT COUNT(*) FROM v_receivable_details WHERE agent_id = ?";
        return jdbc.queryForObject(sql, new Object[]{userId}, Long.class);
    }

    /* Follow-ups / Site Visits Today */
    public Long countFollowUpsToday(UUID userId) {
        String sql = "SELECT COUNT(*) FROM site_visits WHERE user_id = ? AND visit_date = CURRENT_DATE AND status = 'OPEN'";
        return jdbc.queryForObject(sql, new Object[]{userId}, Long.class);
    }

    public Long countSiteVisitsScheduled(UUID userId) {
        String sql = "SELECT COUNT(*) FROM site_visits WHERE user_id = ? AND status = 'OPEN'";
        return jdbc.queryForObject(sql, new Object[]{userId}, Long.class);
    }

    /* Bookings Done */
    public Long countBookingsDone(UUID userId) {
        String sql = "SELECT COUNT(*) FROM v_receivable_details WHERE agent_id = ? AND sale_status IN ('BOOKED','SOLD','COMPLETED')";
        return jdbc.queryForObject(sql, new Object[]{userId}, Long.class);
    }

    /* Commission Earned (Approved) */
    public BigDecimal sumCommissionEarned(UUID userId) {
        String sql = "SELECT COALESCE(SUM(commission_paid),0) FROM v_commission_payable_details WHERE agent_id = ?";
        return jdbc.queryForObject(sql, new Object[]{userId}, BigDecimal.class);
    }

    /* Commission Pending */
    public BigDecimal sumCommissionPending(UUID userId) {
        String sql = "SELECT COALESCE(SUM(commission_payable),0) FROM v_commission_payable_details WHERE agent_id = ?";
        return jdbc.queryForObject(sql, new Object[]{userId}, BigDecimal.class);
    }
}
