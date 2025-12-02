package com.realtors.admin.service;

import com.realtors.admin.dto.ModuleDto;
import com.realtors.admin.dto.PagedResult;
import com.realtors.admin.dto.form.DynamicFormResponseDto;
import com.realtors.admin.dto.form.EditResponseDto;
import com.realtors.admin.dto.form.FeatureColumnMapDto;
import com.realtors.common.EnumConstants;
import com.realtors.common.service.AuditContext;
import com.realtors.common.service.AuditTrailService;
import com.realtors.common.util.AppUtil;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ModuleService extends AbstractBaseService<ModuleDto, UUID>{

    private final JdbcTemplate jdbcTemplate;
    private final AuditTrailService audit;

    public ModuleService(JdbcTemplate jdbcTemplate, AuditTrailService audit) {
    	super(ModuleDto.class, "modules", jdbcTemplate);
        this.jdbcTemplate = jdbcTemplate;
        this.audit = audit;
    }
    
    @Override
	protected String getIdColumn() {
		return "module_id";
	}
    
    public DynamicFormResponseDto getModulesFormData() {
    	audit.auditAsync("modules", null, EnumConstants.FORM.toString(), 
    			AppUtil.getCurrentUserId(), AuditContext.getIpAddress(), AuditContext.getUserAgent());
    	return super.buildDynamicFormResponse();
    }

    public EditResponseDto<ModuleDto> editModulesResponse(UUID roleId) {
        Optional<ModuleDto> opt = super.findById(roleId);
        DynamicFormResponseDto form = super.buildDynamicFormResponse();
        
        audit.auditAsync("modules", null, EnumConstants.EDIT_FORM.toString(), 
    			AppUtil.getCurrentUserId(), AuditContext.getIpAddress(), AuditContext.getUserAgent());
        
        return opt.map(user -> new EditResponseDto<>(user, form))
                  .orElse(null);
    }
    
    // ✅ Create
    public ModuleDto createModule(ModuleDto dto) {
    	ModuleDto data = super.create(dto);
    	audit.auditAsync("modules", data.getModuleId(), EnumConstants.CREATE.toString(), 
    			AppUtil.getCurrentUserId(), AuditContext.getIpAddress(), AuditContext.getUserAgent());
        return data;
    }

    // Get All Modules
    public List<ModuleDto> getAllModules() {
    	List<ModuleDto> list = super.findAll();
    	audit.auditAsync("modules", list.getFirst().getModuleId(), EnumConstants.GET_ALL.toString(), 
    			AppUtil.getCurrentUserId(), AuditContext.getIpAddress(), AuditContext.getUserAgent());
    	return list;
    }
    
    public FeatureColumnMapDto getFeatureData() {
    	return new FeatureColumnMapDto(new String[]{"Module Name", "Description", "Status"},
    			Map.ofEntries(
    				    Map.entry("Module Name", "moduleName"),
    				    Map.entry("Description", "description"),
    				    Map.entry("Status", "status")),
    			"moduleId",
    			true,
    			"table"
    			);
    }
    
    
    // Search modules data
    public List<ModuleDto> searchModules(String searchText) {
    	audit.auditAsync("modules", null, EnumConstants.SEARCH.toString(), 
    			AppUtil.getCurrentUserId(), AuditContext.getIpAddress(), AuditContext.getUserAgent());
    	return super.search(searchText, List.of("module_name", "description"), null);
    }
    
    // Get Paged modules data
    public PagedResult<ModuleDto> getPaginatedModules(int page, int size) {
    	
		/*
		 * return new FeatureListResponseDto<>("Modules", "table",
		 * List.of("Module Name", "Description", "Status"),
		 * Map.ofEntries(Map.entry("Module Name", "moduleName"),
		 * Map.entry("Description", "description"), Map.entry("Status", "status")),
		 * "moduleId", true, // pagination enabled super.findAllPaginated(page, size,
		 * null), // <-- MUST return PagedResult<AppUserDto> null // <-- fully dynamic
		 * lookup map );
		 */
    	PagedResult<ModuleDto> paged = super.findAllPaginated(page, size, null);
    	audit.auditAsync("modules", paged.data().getFirst().getModuleId(), EnumConstants.PAGED.toString(), 
    			AppUtil.getCurrentUserId(), AuditContext.getIpAddress(), AuditContext.getUserAgent());
    	return paged;
    }

    public Optional<ModuleDto> getModuleById(UUID id) {
    	Optional<ModuleDto> opt = super.findById(id);
    	audit.auditAsync("modules", opt.isPresent() ? opt.get().getModuleId() : null, EnumConstants.BY_ID.toString(), 
    			AppUtil.getCurrentUserId(), AuditContext.getIpAddress(), AuditContext.getUserAgent());
    	return opt;
    }

    // ✅ Update
    public ModuleDto updateModule(UUID id, ModuleDto dto) {
    	ModuleDto data = super.update(id, dto);
    	audit.auditAsync("modules", data.getModuleId(), EnumConstants.UPDATE.toString(), 
    			AppUtil.getCurrentUserId(), AuditContext.getIpAddress(), AuditContext.getUserAgent());
    	return data;
    }
    
    public ModuleDto partialUpdate(UUID id, Map<String, Object> dto) {
    	ModuleDto data = super.patch(id, dto);
    	audit.auditAsync("modules", data.getModuleId(), EnumConstants.PATCH.toString(), 
    			AppUtil.getCurrentUserId(), AuditContext.getIpAddress(), AuditContext.getUserAgent());
    	return data;
    }

    // ✅ Soft Delete
    public boolean deleteModule(UUID id) {
    	
    	audit.auditAsync("modules", id, EnumConstants.DELETE.toString(), 
    			AppUtil.getCurrentUserId(), AuditContext.getIpAddress(), AuditContext.getUserAgent());
    	return softDelete(id);
    }
}
