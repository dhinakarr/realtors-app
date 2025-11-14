package com.realtors.admin.service;

import com.realtors.admin.dto.ModuleDto;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.UUID;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ModuleService extends AbstractBaseService<ModuleDto, UUID>{

    private static final Logger logger = LoggerFactory.getLogger(ModuleService.class);
    private final JdbcTemplate jdbcTemplate;

    public ModuleService(JdbcTemplate jdbcTemplate) {
    	super(ModuleDto.class, "modules", jdbcTemplate);
        this.jdbcTemplate = jdbcTemplate;
    }
    
    @Override
	protected String getIdColumn() {
		return "module_id";
	}

    private static final org.springframework.jdbc.core.RowMapper<ModuleDto> MODULE_ROW_MAPPER = (rs, rowNum) -> new ModuleDto(
            UUID.fromString(rs.getString("module_id")),
            rs.getString("module_name"),
            rs.getString("description"),
            rs.getObject("created_at", Timestamp.class),
            rs.getObject("updated_at", Timestamp.class),
            rs.getString("status"),
            (UUID) rs.getObject("created_by"),
            (UUID) rs.getObject("updated_by")
    );

    // ✅ Create
    public ModuleDto createModule(ModuleDto dto) {
        logger.info("Creating new module: {}", dto.getModuleName());
/*
        String sql = """
            INSERT INTO modules (module_name, description, status, created_by, updated_by)
            VALUES (?, ?, 'ACTIVE', ?, ?)
            RETURNING module_id, module_name, description, created_at, updated_at, status, created_by, updated_by
        """;

        return jdbcTemplate.queryForObject(sql,
                MODULE_ROW_MAPPER,
                dto.getModuleName(), dto.getDescription(), currentUserId, currentUserId);
                */
        return super.create(dto);
    }

    public List<ModuleDto> getAllModules() {
    	/*
        String sql = "SELECT * FROM modules WHERE status = 'ACTIVE' ORDER BY created_at DESC";
        return jdbcTemplate.query(sql, MODULE_ROW_MAPPER);
        */
    	return super.findAll();
    }

    public Optional<ModuleDto> getModuleById(UUID id) {
    	/*
        String sql = "SELECT * FROM modules WHERE module_id = ? AND status = 'ACTIVE'";
        List<ModuleDto> result = jdbcTemplate.query(sql, MODULE_ROW_MAPPER, id);
        return result.stream().findFirst();
        */
    	return super.findById(id);
    }

    // ✅ Update
    public ModuleDto updateModule(UUID id, ModuleDto dto) {
    	/*
        int updated = jdbcTemplate.update("""
            UPDATE modules
            SET module_name = ?, description = ?, updated_at = CURRENT_TIMESTAMP, updated_by = ?
            WHERE module_id = ? AND status = 'ACTIVE'
        """, dto.getModuleName(), dto.getDescription(), currentUserId, id);

        if (updated == 0) {
            throw new ResourceNotFoundException("Module not found or inactive: " + id);
        }

        return getModuleById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Module not found after update: " + id));
                */
    	return super.update(id, dto);
    }
    
    public ModuleDto partialUpdate(UUID id, Map<String, Object> dto) {
//        GenericUpdateUtil.partialUpdate("modules", "module_id", id, dto, jdbcTemplate);
    	return patch(id, dto);
    }

    // ✅ Soft Delete
    public boolean deleteModule(UUID id) {
    	/*
        int updated = jdbcTemplate.update("""
            UPDATE modules
            SET status = 'INACTIVE', updated_at = CURRENT_TIMESTAMP, updated_by = ?
            WHERE module_id = ? AND status = 'ACTIVE'
        """, currentUserId, id);

        if (updated == 0) {
            throw new ResourceNotFoundException("Module not found or already inactive: " + id);
        }

        logger.info("Module {} marked as INACTIVE", id);
        */
    	return softDelete(id);
    }
}
