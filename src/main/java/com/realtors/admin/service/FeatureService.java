package com.realtors.admin.service;

import com.realtors.admin.dto.FeatureDto;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.*;

@Slf4j
@Service
public class FeatureService extends AbstractBaseService<FeatureDto, UUID>{

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public FeatureService(JdbcTemplate jdbcTemplate) {
        super(FeatureDto.class, "features", jdbcTemplate);
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    protected String getIdColumn() {
        return "feature_id";
    }
    
    private static final org.springframework.jdbc.core.RowMapper<FeatureDto> featureRowMapper = (rs, rowNum) -> {
    	FeatureDto dto = new FeatureDto();
        dto.setFeatureId((UUID) rs.getObject("feature_id"));
        dto.setModuleId((UUID) rs.getObject("module_id"));
        dto.setFeatureName(rs.getString("feature_name"));
        dto.setDescription(rs.getString("description"));
        dto.setUrl(rs.getString("url"));
        dto.setStatus(rs.getString("status"));
        dto.setCreatedAt(rs.getObject("created_at", Timestamp.class));
        dto.setUpdatedAt(rs.getObject("updated_at", Timestamp.class));
        return dto;
    };

    // CREATE
    public FeatureDto createFeature(FeatureDto dto) {
        log.info("Creating new feature: {}", dto.getFeatureName());
        return super.create(dto);
        /*
        String sql = """
                INSERT INTO features (
                    module_id, feature_name, description, url,
                    status, created_at, updated_at, created_by, updated_by
                )
                VALUES (?, ?, ?, ?, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, ?, ?)
                RETURNING feature_id, module_id, feature_name, description, url, status, created_at, updated_at
            """;
        return jdbcTemplate.queryForObject(
                sql,
                featureRowMapper,
                dto.getModuleId(),
                dto.getFeatureName(),
                dto.getDescription(),
                dto.getUrl(),
                currentUserId,
                currentUserId
        );
        */
    }

    // READ ALL (Active only)
    public List<FeatureDto> getAllFeatures() {
        log.info("Fetching all active features");
        /*
        String sql = "SELECT * FROM features WHERE status = 'ACTIVE' ORDER BY created_at DESC";

        List<FeatureDto> list = jdbcTemplate.query(sql, featureRowMapper);
//        return ResponseEntity.ok(new ApiResponse<>(true, "Features fetched successfully", list));
        return list;
        */
        return super.findAll();
    }

    // READ BY ID
    public Optional<FeatureDto> getFeatureById(UUID featureId) {
        log.info("Fetching feature by ID: {}", featureId);
/*
        String sql = "SELECT * FROM features WHERE feature_id = ? AND status = 'ACTIVE'";
        List<FeatureDto> results = jdbcTemplate.query(sql, featureRowMapper, featureId);

        Optional<FeatureDto> feature = results.stream().findFirst();
        return feature;
        */
        return super.findById(featureId);
    }

    // UPDATE
    public FeatureDto updateFeature(UUID featureId, FeatureDto dto) {
        log.info("Updating feature ID: {}", featureId);
/*
        int updated = jdbcTemplate.update("""
                UUPDATE features
            SET module_id = ?, feature_name = ?, description = ?, url = ?, updated_by=?, updated_at = CURRENT_TIMESTAMP
            WHERE feature_id = ? AND status = 'ACTIVE'
            """, dto.getModuleId(), dto.getFeatureName(), dto.getDescription(), dto.getUrl(), currentUserId, featureId);
        if (updated == 0) {
            throw new ResourceNotFoundException("Module not found or inactive: " + featureId);
        }

        return getFeatureById(featureId)
                .orElseThrow(() -> new ResourceNotFoundException("Feature not found after update: " + featureId));
                */
        return super.update(featureId, dto);
    }

    public FeatureDto partialUpdate(UUID id, Map<String, Object> dto) {
        return super.patch(id, dto);
    }
    
    // SOFT DELETE
    public boolean deleteFeature(UUID featureId) {
        log.info("Soft deleting feature ID: {}", featureId);
        /*
        String sql = "UPDATE features SET status = 'INACTIVE', updated_at = CURRENT_TIMESTAMP WHERE feature_id = ?";

        int updated = jdbcTemplate.update(sql, featureId);
        if (updated > 0) {
        	throw new ResourceNotFoundException("Module not found or already inactive: " + featureId);
        } */
        log.info("Feature {} marked as INACTIVE", featureId);
        return super.softDelete(featureId);
    }
}

