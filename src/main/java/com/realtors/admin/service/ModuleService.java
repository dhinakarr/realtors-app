package com.realtors.admin.service;

import com.realtors.admin.dto.ModuleDto;
import com.realtors.admin.dto.PagedResult;
import com.realtors.admin.dto.form.DynamicFormResponseDto;
import com.realtors.admin.dto.form.EditResponseDto;
import com.realtors.admin.dto.form.FeatureColumnMapDto;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ModuleService extends AbstractBaseService<ModuleDto, UUID>{

    private final JdbcTemplate jdbcTemplate;

    public ModuleService(JdbcTemplate jdbcTemplate) {
    	super(ModuleDto.class, "modules", jdbcTemplate);
        this.jdbcTemplate = jdbcTemplate;
    }
    
    @Override
	protected String getIdColumn() {
		return "module_id";
	}
    
    public DynamicFormResponseDto getModulesFormData() {
    	return super.buildDynamicFormResponse();
    }

    public EditResponseDto<ModuleDto> editModulesResponse(UUID roleId) {
        Optional<ModuleDto> opt = super.findById(roleId);
        DynamicFormResponseDto form = super.buildDynamicFormResponse();
        
        return opt.map(user -> new EditResponseDto<>(user, form))
                  .orElse(null);
    }
    
    // ✅ Create
    public ModuleDto createModule(ModuleDto dto) {
        return super.create(dto);
    }

    // Get All Modules
    public List<ModuleDto> getAllModules() {
    	return super.findAll();
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
    	return super.findAllPaginated(page, size, null);
    }

    public Optional<ModuleDto> getModuleById(UUID id) {
    	return super.findById(id);
    }

    // ✅ Update
    public ModuleDto updateModule(UUID id, ModuleDto dto) {
    	return super.update(id, dto);
    }
    
    public ModuleDto partialUpdate(UUID id, Map<String, Object> dto) {
    	return patch(id, dto);
    }

    // ✅ Soft Delete
    public boolean deleteModule(UUID id) {
    	return softDelete(id);
    }
}
