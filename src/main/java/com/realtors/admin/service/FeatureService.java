package com.realtors.admin.service;

import com.realtors.admin.dto.FeatureDto;
import com.realtors.admin.dto.PagedResult;
import com.realtors.admin.dto.form.DynamicFormResponseDto;
import com.realtors.admin.dto.form.EditResponseDto;

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
        addDependentLookup("module_id", "modules", "module_id", "module_name", "moduleName");
    }

    @Override
    protected String getIdColumn() {
        return "feature_id";
    }
    
    //List<LookupDefinition> lookupDefs = List.of(new LookupDefinition("modules", "modules", "module_id", "module_name", ""));
    
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
    
    public DynamicFormResponseDto getRolesFormData() {
    	return super.buildDynamicFormResponse();
    }
    
    public EditResponseDto<FeatureDto> editRolesResponse(UUID roleId) {
        Optional<FeatureDto> opt = super.findById(roleId);
        DynamicFormResponseDto form = super.buildDynamicFormResponse();
        return opt.map(user -> new EditResponseDto<>(user, form))
                  .orElse(null);
    }

    // CREATE
    public FeatureDto createFeature(FeatureDto dto) {
        return super.create(dto);
    }

    // READ ALL (Active only)
    public List<FeatureDto> getAllFeatures() {
        return super.findAll();
    }
    
    // SEARCH ALL (Active only)
    public List<FeatureDto> searchFeatures(String searchText) {
        return super.search(searchText, List.of("feature_name", "description"), null);
    }
    
    // SEARCH ALL (Active only)
    public PagedResult<FeatureDto> getPaginated(int page, int size) {
		/*
		 * return new FeatureListResponseDto<>( "Features", "table",
		 * List.of("Feature Name", "Description", "Module Name", "Status"),
		 * Map.ofEntries( Map.entry("Feature Name", "featureName"),
		 * Map.entry("Module Name", "moduleName"), Map.entry("Description",
		 * "description"), Map.entry("Status", "status")), "featureId", true, //
		 * pagination enabled super.findAllPaginated(page, size, null), // <-- MUST
		 * return PagedResult<AppUserDto> super.getLookupData(lookupDefs) // <-- fully
		 * dynamic lookup map );
		 */
    	return super.findAllPaginated(page, size, null);
    }

    // READ BY ID
    public Optional<FeatureDto> getFeatureById(UUID featureId) {
        return super.findById(featureId);
    }

    // UPDATE
    public FeatureDto updateFeature(UUID featureId, FeatureDto dto) {
        return super.update(featureId, dto);
    }

    public FeatureDto partialUpdate(UUID id, Map<String, Object> dto) {
        return super.patch(id, dto);
    }
    
    // SOFT DELETE
    public boolean deleteFeature(UUID featureId) {
        return super.softDelete(featureId);
    }
}

