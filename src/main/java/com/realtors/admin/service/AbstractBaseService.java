package com.realtors.admin.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import com.realtors.admin.dto.PagedResult;
import com.realtors.common.util.AppUtil;
import com.realtors.common.util.GenericInsertUtil;
import com.realtors.common.util.GenericUpdateUtil;
import com.realtors.common.util.GenericUpdatesUtil;
import com.realtors.common.util.JsonAwareRowMapper;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public abstract class AbstractBaseService<T, ID> implements BaseService<T, ID> {

    @Autowired
    protected JdbcTemplate jdbcTemplate;
    protected final Logger logger = Logger.getLogger(getClass().getName());

    private final Class<T> dtoClass;
    private final String tableName;
    

    protected AbstractBaseService(Class<T> dtoClass, String tableName, JdbcTemplate jdbcTemplate) {
        this.dtoClass = dtoClass;
        this.tableName = tableName;
        this.jdbcTemplate = jdbcTemplate;
    }

    // Every child service defines its primary key column name
    protected abstract String getIdColumn();
    
    @Override
    public T create(T dto) {
    	UUID currentUser = AppUtil.getCurrentUserId();
    	
        Map<String, Object> values = GenericInsertUtil.toColumnMap(dto, getIdColumn());
        values.remove(getIdColumn());
        
        values.put("status", "ACTIVE");
        // Add audit fields automatically
//        if (currentUser != null) {
            values.put("created_by", currentUser);
            values.put("updated_by", currentUser);
//        }
        return GenericInsertUtil.insertGeneric(
                tableName,
                dto,
                jdbcTemplate,
                dtoClass,
                getIdColumn()
        );
    }

    @Override
    public Optional<T> findById(ID id) {
        String sql = "SELECT * FROM " + tableName + " WHERE " + getIdColumn() + " = ?";
        List<T> list = jdbcTemplate.query(sql, new JsonAwareRowMapper<>(dtoClass), id);
        return list.stream().findFirst();
    }

    @Override
    public List<T> findAll() {
        String sql = "SELECT * FROM " + tableName + " WHERE status = 'ACTIVE'";
        return jdbcTemplate.query(sql, new JsonAwareRowMapper<>(dtoClass));
    }
    
    /**
     * Fetches all records regardless of status (ACTIVE / INACTIVE / others).
     */
    public List<T> findAllWithInactive() {
        String sql = "SELECT * FROM " + tableName + " ORDER BY updated_at DESC";
        logger.info(() -> "Fetching all records (including INACTIVE) from table: " + tableName);
        return jdbcTemplate.query(sql, new JsonAwareRowMapper<>(dtoClass));
    }

    @Override
    public T update(ID id, T dto) {
    	
    	Object idValue = id;
    	
        return GenericUpdateUtil.updateGeneric(
        		tableName,
                dto, // can be DTO or Map<String, Object>
                jdbcTemplate,
                dtoClass,
                getIdColumn(),
                idValue
        );
    }
    
    public T patch(Object id, Map<String, Object> updates) {
    	
    	if (updates == null || updates.isEmpty()) {
            throw new IllegalArgumentException("No fields to update");
        }

        String idColumn = getIdColumn(); // e.g. "id" or "role_id, permission_id"
        Object idValue;

        // Handle composite key
        if (idColumn.contains(",")) {
            List<String> idColumns = Arrays.stream(idColumn.split(","))
                    .map(String::trim)
                    .collect(Collectors.toList());

            if (id instanceof Map) {
                idValue = id;
            } else if (id instanceof List) {
                idValue = id;
            } else {
                // Try to auto-map from dto or updates if possible (optional)
                throw new IllegalArgumentException(
                    "Composite key table requires Map or List for idValue. " +
                    "Example: Map.of('role_id', val1, 'permission_id', val2)"
                );
            }
        }
    	
        return GenericUpdateUtil.patchGeneric(
                tableName,
                updates,
                jdbcTemplate,
                dtoClass,
                getIdColumn(),
                id
        );
    }
  
    @Override
    public boolean softDelete(ID id) {
        String sql = "UPDATE " + tableName + " SET status = 'INACTIVE', updated_at = ?, updated_by = ? WHERE " + getIdColumn() + " = ?";
        UUID currentUser = AppUtil.getCurrentUserId();
        return jdbcTemplate.update(sql, OffsetDateTime.now(), currentUser, id) > 0;
    }
    /*
    @Override
    public T update(ID id, T dto) {
        Map<String, Object> values = GenericInsertUtil.toColumnMap(dto, getIdColumn());

        // audit fields
        UUID currentUser = AppUtil.getCurrentUserId();
        if (currentUser != null) values.put("updated_by", currentUser);
        values.put("updated_at", OffsetDateTime.now());

        // Build SQL dynamically
        StringBuilder sql = new StringBuilder("UPDATE " + tableName + " SET ");
        List<Object> params = new ArrayList<>();

        for (Map.Entry<String, Object> entry : values.entrySet()) {
            sql.append(entry.getKey()).append(" = ?, ");
            params.add(entry.getValue());
        }
        sql.setLength(sql.length() - 2); // remove last comma

        sql.append(" WHERE ").append(getIdColumn()).append(" = ?");
        params.add(id);

        jdbcTemplate.update(sql.toString(), params.toArray());

        return findById(id).orElse(null);
    }

    

    @Override
    public T patch(ID id, Map<String, Object> updates) {
        // audit fields
        UUID currentUser = AppUtil.getCurrentUserId();
        if (currentUser != null) updates.put("updated_by", currentUser);
        updates.put("updated_at", OffsetDateTime.now());

        StringBuilder sql = new StringBuilder("UPDATE " + tableName + " SET ");
        List<Object> params = new ArrayList<>();

        for (Map.Entry<String, Object> entry : updates.entrySet()) {
            sql.append(entry.getKey()).append(" = ?, ");
            params.add(entry.getValue());
        }
        sql.setLength(sql.length() - 2); // remove last comma

        sql.append(" WHERE ").append(getIdColumn()).append(" = ?");
        params.add(id);

        jdbcTemplate.update(sql.toString(), params.toArray());

        return findById(id).orElse(null);
    }
*/
    // --- helpers ---
    protected T mapSingle(Map<String, Object> result) {
        if (result == null || result.isEmpty()) return null;
        try {
            T instance = dtoClass.getDeclaredConstructor().newInstance();
            result.forEach((k, v) -> {
                try {
                    String fieldName = toCamel(k);
                    var field = dtoClass.getDeclaredField(fieldName);
                    field.setAccessible(true);
                    field.set(instance, v);
                } catch (Exception ignored) {}
            });
            return instance;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected String toSnake(String name) {
        return name.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
    }

    protected String toCamel(String name) {
        StringBuilder sb = new StringBuilder();
        boolean nextUpper = false;
        for (char c : name.toCharArray()) {
            if (c == '_') nextUpper = true;
            else sb.append(nextUpper ? Character.toUpperCase(c) : c);
            nextUpper = false;
        }
        return sb.toString();
    }
    
    /**
     * Generic pagination method for all tables.
     */
    public PagedResult<T> findAllPaginated(int page, int size, String status) {
        if (status == null || status.isEmpty()) status = "ACTIVE";
        int offset = (page - 1) * size;

        String sql = "SELECT * FROM " + tableName + " WHERE status = ? ORDER BY updated_at DESC LIMIT ? OFFSET ?";
        String countSql = "SELECT COUNT(*) FROM " + tableName + " WHERE status = ?";

        List<T> results = jdbcTemplate.query(sql, new JsonAwareRowMapper<>(dtoClass), status, size, offset);
        int total = jdbcTemplate.queryForObject(countSql, Integer.class, status);

        return new PagedResult<>(
            results,
            page,
            size,
            total,
            (int) Math.ceil((double) total / size)
        );
    }

    public int countByStatus(String status) {
        String sql = "SELECT COUNT(*) FROM " + tableName + " WHERE status = ?";
        return jdbcTemplate.queryForObject(sql, Integer.class, status);
    }
    
    /**
     * Generic search across multiple fields like Google-style fuzzy search.
     */
    public List<T> search(String searchText, List<String> searchFields, String status) {
        if (status == null || status.isEmpty()) status = "ACTIVE";

        if (searchText == null || searchText.isBlank()) {
            return findAll();
        }

        StringBuilder whereClause = new StringBuilder(" WHERE status = ? AND (");
        List<Object> params = new ArrayList<>();
        params.add(status);

        for (int i = 0; i < searchFields.size(); i++) {
            whereClause.append(searchFields.get(i)).append(" ILIKE ?");
            params.add("%" + searchText + "%");
            if (i < searchFields.size() - 1) whereClause.append(" OR ");
        }
        whereClause.append(")");

        String sql = "SELECT * FROM " + tableName + whereClause + " ORDER BY updated_at DESC LIMIT ? ";
        return jdbcTemplate.query(sql, new JsonAwareRowMapper<>(dtoClass), params.toArray());
    }

    /**
     * Generic search across multiple fields like Google-style fuzzy search.
     */
    public PagedResult<T> searchPage(String searchText, List<String> searchFields, int page, int size, String status) {
        if (status == null || status.isEmpty()) status = "ACTIVE";
        int offset = (page - 1) * size;

        if (searchText == null || searchText.isBlank()) {
            return findAllPaginated(page, size, status);
        }

        StringBuilder whereClause = new StringBuilder(" WHERE status = ? AND (");
        List<Object> params = new ArrayList<>();
        params.add(status);

        for (int i = 0; i < searchFields.size(); i++) {
            whereClause.append(searchFields.get(i)).append(" ILIKE ?");
            params.add("%" + searchText + "%");
            if (i < searchFields.size() - 1) whereClause.append(" OR ");
        }
        whereClause.append(")");

        String sql = "SELECT * FROM " + tableName + whereClause + " ORDER BY updated_at DESC LIMIT ? OFFSET ?";
        String countSql = "SELECT COUNT(*) FROM " + tableName + whereClause;

        params.add(size);
        params.add(offset);

        List<T> results = jdbcTemplate.query(sql, new JsonAwareRowMapper<>(dtoClass), params.toArray());
        int total = jdbcTemplate.queryForObject(countSql, Integer.class, params.subList(0, params.size() - 2).toArray());

        return new PagedResult<>(
            results,
            page,
            size,
            total,
            (int) Math.ceil((double) total / size)
        );
    }
    
    /**
     * Fetch all records with pagination, regardless of status.
     */
    public PagedResult<T> findAllIncludingInactivePaginated(int page, int size) {
        int offset = (page - 1) * size;

        String sql = "SELECT * FROM " + tableName + " ORDER BY updated_at DESC LIMIT ? OFFSET ?";
        String countSql = "SELECT COUNT(*) FROM " + tableName;

        List<T> results = jdbcTemplate.query(sql, new JsonAwareRowMapper<>(dtoClass), size, offset);
        int total = jdbcTemplate.queryForObject(countSql, Integer.class);

        return new PagedResult<>(
            results,
            page,
            size,
            total,
            (int) Math.ceil((double) total / size)
        );
    }
}

