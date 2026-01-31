package com.realtors.admin.service;

import com.realtors.admin.dto.ModuleDto;
import com.realtors.admin.dto.PagedResult;
import com.realtors.admin.dto.form.DynamicFormResponseDto;
import com.realtors.admin.dto.form.EditResponseDto;
import com.realtors.admin.dto.form.FeatureColumnMapDto;
import com.realtors.common.EnumConstants;
import com.realtors.common.service.AuditTrailService;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ModuleService extends AbstractBaseService<ModuleDto, UUID> {

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
		return super.buildDynamicFormResponse();
	}

	public EditResponseDto<ModuleDto> editModulesResponse(UUID roleId) {
		Optional<ModuleDto> opt = super.findById(roleId);
		DynamicFormResponseDto form = super.buildDynamicFormResponse();

		return opt.map(user -> new EditResponseDto<>(user, form)).orElse(null);
	}

	// ✅ Create
	public ModuleDto createModule(ModuleDto dto) {
		ModuleDto data = super.create(dto);
		audit.auditAsync("modules", data.getModuleId(), EnumConstants.CREATE);
		return data;
	}

	// Get All Modules
	public List<ModuleDto> getAllModules() {
		List<ModuleDto> list = super.findAll();
		return list;
	}

	public FeatureColumnMapDto getFeatureData() {
		return new FeatureColumnMapDto(new String[] { "Module Name", "Description", "Status" },
				Map.ofEntries(Map.entry("Module Name", "moduleName"), Map.entry("Description", "description"),
						Map.entry("Status", "status")),
				"moduleId", true, "table");
	}

	// Search modules data
	public List<ModuleDto> searchModules(String searchText) {
		return super.search(searchText, List.of("module_name", "description"), null);
	}

	// Get Paged modules data
	public PagedResult<ModuleDto> getPaginatedModules(int page, int size) {
		PagedResult<ModuleDto> paged = super.findAllPaginated(page, size, null);
		return paged;
	}

	public Optional<ModuleDto> getModuleById(UUID id) {
		Optional<ModuleDto> opt = super.findById(id);
		return opt;
	}

	// ✅ Update
	public ModuleDto updateModule(UUID id, ModuleDto dto) {
		ModuleDto data = super.update(id, dto);
		audit.auditAsync("modules", data.getModuleId(), EnumConstants.UPDATE);
		return data;
	}

	public ModuleDto partialUpdate(UUID id, Map<String, Object> dto) {
		ModuleDto data = super.patch(id, dto);
		audit.auditAsync("modules", data.getModuleId(), EnumConstants.PATCH);
		return data;
	}

	// ✅ Soft Delete
	public boolean deleteModule(UUID id) {
		audit.auditAsync("modules", id, EnumConstants.DELETE);
		return softDelete(id);
	}
}
