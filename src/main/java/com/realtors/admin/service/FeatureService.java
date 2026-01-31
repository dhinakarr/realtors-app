package com.realtors.admin.service;

import com.realtors.admin.dto.FeatureDto;
import com.realtors.admin.dto.PagedResult;
import com.realtors.admin.dto.form.DynamicFormResponseDto;
import com.realtors.admin.dto.form.EditResponseDto;
import com.realtors.common.EnumConstants;
import com.realtors.common.service.AuditTrailService;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
public class FeatureService extends AbstractBaseService<FeatureDto, UUID>{

    @Autowired
    private final JdbcTemplate jdbcTemplate;
    private final AuditTrailService audit; 

    public FeatureService(JdbcTemplate jdbcTemplate, AuditTrailService audit) {
        super(FeatureDto.class, "features", jdbcTemplate);
        this.jdbcTemplate = jdbcTemplate;
        this.audit = audit;
        addDependentLookup("module_id", "modules", "module_id", "module_name", "moduleName");
    }
    
    @Override
    protected String getIdColumn() {
        return "feature_id";
    }
    
    //List<LookupDefinition> lookupDefs = List.of(new LookupDefinition("modules", "modules", "module_id", "module_name", ""));
    
    public DynamicFormResponseDto getRolesFormData() {
    	DynamicFormResponseDto dto = super.buildDynamicFormResponse();
    	return dto;
    }
    
    public EditResponseDto<FeatureDto> editRolesResponse(UUID roleId) {
        Optional<FeatureDto> opt = super.findById(roleId);
        DynamicFormResponseDto form = super.buildDynamicFormResponse();
        
        return opt.map(user -> new EditResponseDto<>(user, form))
                  .orElse(null);
    }

    // CREATE
    public FeatureDto createFeature(FeatureDto dto) {
    	FeatureDto data = super.create(dto);
    	audit.auditAsync("features", data.getFeatureId(), EnumConstants.CREATE);
        return data;
    }

    // READ ALL (Active only)
    public List<FeatureDto> getAllFeatures() {
    	List<FeatureDto> list = super.findAll();
        return list;
    }
    
    // SEARCH ALL (Active only)
    public List<FeatureDto> searchFeatures(String searchText) {
        return super.search(searchText, List.of("feature_name", "description"), null);
    }
    
    // SEARCH ALL (Active only)
    public PagedResult<FeatureDto> getPaginated(int page, int size) {
    	PagedResult<FeatureDto> paged = super.findAllPaginated(page, size, null);
    	return paged;
    }

    // READ BY ID
    public Optional<FeatureDto> getFeatureById(UUID featureId) {
    	Optional<FeatureDto> opt = super.findById(featureId);
        return opt;
    }

    // UPDATE
    public FeatureDto updateFeature(UUID featureId, FeatureDto dto) {
    	FeatureDto data = super.update(featureId, dto);
    	audit.auditAsync("features", data.getFeatureId(), EnumConstants.UPDATE);
        return data;
    }

    public FeatureDto partialUpdate(UUID id, Map<String, Object> dto) {
    	FeatureDto data = super.patch(id, dto);
    	audit.auditAsync("features", data.getFeatureId(), EnumConstants.PATCH);
        return data;
    }
    
    // SOFT DELETE
    public boolean deleteFeature(UUID featureId) {
    	audit.auditAsync("features", featureId, EnumConstants.DELETE);
        return super.softDelete(featureId);
    }
}

