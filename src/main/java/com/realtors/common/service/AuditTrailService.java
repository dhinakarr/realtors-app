package com.realtors.common.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.realtors.common.EnumConstants;
import com.realtors.common.util.AppUtil;

import java.util.UUID;

@Service
public class AuditTrailService {
    private static final Logger logger = LoggerFactory.getLogger(AuditTrailService.class);
    private final JdbcTemplate jdbcTemplate;

    public AuditTrailService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
    
    @Async("auditExecutor")
    public void auditAsync(String tableName, UUID recordId, EnumConstants action) {
    	String ipAddress = AuditContext.getIpAddress();
    	String userAgent = AuditContext.getUserAgent();
    	UUID performedBy = AppUtil.getCurrentUserId();
    	auditAsync(tableName, recordId, action.toString(), performedBy, ipAddress, userAgent);
    }
    
    private void auditAsync(String tableName, UUID recordId, String action, UUID performedBy,
                         String ipAddress, String userAgent) {
        try {
            String sql = """
                INSERT INTO audit_trail
                    (table_name, record_id, action, performed_by, ip_address, user_agent)
                VALUES (?, ?, ?, ?, ?, ?)
            """;
            jdbcTemplate.update(sql,
                    tableName,
                    recordId,
                    action,
                    performedBy,
                    ipAddress,
                    userAgent
            );
        } catch (Exception e) {
            logger.error("Failed to insert audit log: {}", e.getMessage(), e);
        }
    }
}

