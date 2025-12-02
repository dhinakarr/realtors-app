package com.realtors.admin.service;

import com.realtors.admin.dto.FeatureDto;
import com.realtors.admin.dto.PagedResult;
import com.realtors.admin.dto.form.DynamicFormResponseDto;
import com.realtors.admin.dto.form.EditResponseDto;
import com.realtors.common.EnumConstants;
import com.realtors.common.service.AuditContext;
import com.realtors.common.service.AuditTrailService;
import com.realtors.common.util.AppUtil;

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
    	DynamicFormResponseDto dto = super.buildDynamicFormResponse();
    	audit.auditAsync("features", null, EnumConstants.FORM.toString(), 
    			AppUtil.getCurrentUserId(), AuditContext.getIpAddress(), AuditContext.getUserAgent());
    	
    	return dto;
    }
    
    public EditResponseDto<FeatureDto> editRolesResponse(UUID roleId) {
        Optional<FeatureDto> opt = super.findById(roleId);
        DynamicFormResponseDto form = super.buildDynamicFormResponse();
        
        audit.auditAsync("features", roleId, EnumConstants.EDIT_FORM.toString(), 
    			AppUtil.getCurrentUserId(), AuditContext.getIpAddress(), AuditContext.getUserAgent());
        
        return opt.map(user -> new EditResponseDto<>(user, form))
                  .orElse(null);
    }

    // CREATE
    public FeatureDto createFeature(FeatureDto dto) {
    	FeatureDto data = super.create(dto);
    	audit.auditAsync("features", data.getFeatureId(), EnumConstants.CREATE.toString(), 
    			AppUtil.getCurrentUserId(), AuditContext.getIpAddress(), AuditContext.getUserAgent());
        return data;
    }

    // READ ALL (Active only)
    public List<FeatureDto> getAllFeatures() {
    	List<FeatureDto> list = super.findAll();
    	audit.auditAsync("features", list.getFirst().getFeatureId(), EnumConstants.GET_ALL.toString(), 
    			AppUtil.getCurrentUserId(), AuditContext.getIpAddress(), AuditContext.getUserAgent());
        return list;
    }
    
    // SEARCH ALL (Active only)
    public List<FeatureDto> searchFeatures(String searchText) {
    	audit.auditAsync("features", null, EnumConstants.SEARCH.toString(), 
    			AppUtil.getCurrentUserId(), AuditContext.getIpAddress(), AuditContext.getUserAgent());
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
    	PagedResult<FeatureDto> paged = super.findAllPaginated(page, size, null);
    	audit.auditAsync("features", paged.data().getFirst().getFeatureId(), EnumConstants.PAGED.toString(), 
    			AppUtil.getCurrentUserId(), AuditContext.getIpAddress(), AuditContext.getUserAgent());
    	return paged;
    }

    // READ BY ID
    public Optional<FeatureDto> getFeatureById(UUID featureId) {
    	Optional<FeatureDto> opt = super.findById(featureId);
    	audit.auditAsync("features", opt.isPresent() ? opt.get().getFeatureId() : null, EnumConstants.BY_ID.toString(), 
    			AppUtil.getCurrentUserId(), AuditContext.getIpAddress(), AuditContext.getUserAgent());
        return opt;
    }

    // UPDATE
    public FeatureDto updateFeature(UUID featureId, FeatureDto dto) {
    	FeatureDto data = super.update(featureId, dto);
    	audit.auditAsync("features", data.getFeatureId(), EnumConstants.UPDATE.toString(), 
    			AppUtil.getCurrentUserId(), AuditContext.getIpAddress(), AuditContext.getUserAgent());
        return data;
    }

    public FeatureDto partialUpdate(UUID id, Map<String, Object> dto) {
    	FeatureDto data = super.patch(id, dto);
    	audit.auditAsync("features", data.getFeatureId(), EnumConstants.PATCH.toString(), 
    			AppUtil.getCurrentUserId(), AuditContext.getIpAddress(), AuditContext.getUserAgent());
        return data;
    }
    
    // SOFT DELETE
    public boolean deleteFeature(UUID featureId) {
    	audit.auditAsync("features", featureId, EnumConstants.DELETE.toString(), 
    			AppUtil.getCurrentUserId(), AuditContext.getIpAddress(), AuditContext.getUserAgent());
        return super.softDelete(featureId);
    }
}

