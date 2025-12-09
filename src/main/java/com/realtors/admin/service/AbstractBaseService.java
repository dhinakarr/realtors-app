package com.realtors.admin.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.realtors.admin.dto.AppUserDto;
import com.realtors.admin.dto.PagedResult;
import com.realtors.admin.dto.form.DynamicFormMetaRow;
import com.realtors.admin.dto.form.DynamicFormResponseDto;
import com.realtors.admin.dto.form.LookupDefinition;
import com.realtors.common.util.AppUtil;
import com.realtors.common.util.GenericInsertUtil;
import com.realtors.common.util.GenericUpdateUtil;
import com.realtors.common.util.JsonAwareRowMapper;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.logging.Logger;

public abstract class AbstractBaseService<T, ID> implements BaseService<T, ID> {

	@Autowired
	protected JdbcTemplate jdbcTemplate;
	protected final Logger logger = Logger.getLogger(getClass().getName());

	private final Class<T> dtoClass;
	private final String tableName;
	protected Set<String> excludeColumns = new HashSet();

	protected AbstractBaseService(Class<T> dtoClass, String tableName, JdbcTemplate jdbcTemplate) {
		this.dtoClass = dtoClass;
		this.tableName = tableName;
		this.jdbcTemplate = jdbcTemplate;
	}

	protected AbstractBaseService(Class<T> dtoClass, String tableName, JdbcTemplate jdbcTemplate,
			Set<String> removeColumns) {
		this.dtoClass = dtoClass;
		this.tableName = tableName;
		this.jdbcTemplate = jdbcTemplate;
		this.excludeColumns = removeColumns;
	}

	// Every child service defines its primary key column name
	protected abstract String getIdColumn();

	/**
	 * Map of foreign key column -> lookup table info Example: "role_id" -> roles
	 * table, "role_id" as id, "role_name" as name
	 */
	protected Map<String, DependentLookup> dependentLookups = new HashMap<>();

	public static class DependentLookup {
		public final String tableName;
		public final String idColumn;
		public final String nameColumn;
		public final String resultAlias;

		public DependentLookup(String tableName, String idColumn, String nameColumn, String resultAlias) {
			this.tableName = tableName;
			this.idColumn = idColumn;
			this.nameColumn = nameColumn;
			this.resultAlias = resultAlias;
		}
	}

	protected void addDependentLookup(String fkColumn, String lookupTable, String idColumn, String nameColumn,
			String resultAlias) {
		dependentLookups.put(fkColumn, new DependentLookup(lookupTable, idColumn, nameColumn, resultAlias));
	}

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
		return GenericInsertUtil.insertGeneric(tableName, dto, jdbcTemplate, dtoClass, getIdColumn());
	}

	// ---------------- CREATE ----------------
	public AppUserDto createWithFiles(Map<String, Object> data) {
		// You can use a GenericInsertUtil that supports files
		return GenericInsertUtil.insertGenericWithFileSupport(this.tableName, data, jdbcTemplate, AppUserDto.class,
				this.excludeColumns);
	}

	@Override
	public Optional<T> findById(ID id) {
		String sql = buildSelectWithLookups("t") + " WHERE t." + getIdColumn() + " = ?";
//        String sql = "SELECT * FROM " + tableName + " WHERE " + getIdColumn() + " = ?";
		List<T> list = jdbcTemplate.query(sql, new JsonAwareRowMapper<>(dtoClass), id);
		return list.stream().findFirst();
	}

	@Override
	public List<T> findAll() {
//        String sql = "SELECT * FROM " + tableName + " WHERE status = 'ACTIVE'";
		String sql = buildSelectWithLookups("t") + " WHERE t.status = 'ACTIVE'";
		return jdbcTemplate.query(sql, new JsonAwareRowMapper<>(dtoClass));
	}

	/**
	 * Fetches all records regardless of status (ACTIVE / INACTIVE / others).
	 */
	public List<T> findAllWithInactive() {
//        String sql = "SELECT * FROM " + tableName + " ORDER BY updated_at DESC";
		String sql = buildSelectWithLookups("t") + " ORDER BY t.updated_at DESC";
		logger.info(() -> "Fetching all records (including INACTIVE) from table: " + tableName);
		return jdbcTemplate.query(sql, new JsonAwareRowMapper<>(dtoClass));
	}

	@Override
	public T update(ID id, T dto) {
		Object idValue = id;
		return GenericUpdateUtil.updateGeneric(tableName, dto, // can be DTO or Map<String, Object>
				jdbcTemplate, dtoClass, getIdColumn(), idValue);
	}

	public T patchUpdateWithFile(ID id, Map<String, Object> updates) {
		return GenericUpdateUtil.patchGenericWithFileSupport(tableName, updates, jdbcTemplate, dtoClass, getIdColumn(),
				id);
	}

	public T patch(Object id, Map<String, Object> updates) {
		if (updates == null || updates.isEmpty()) {
			throw new IllegalArgumentException("No fields to update");
		}
		return GenericUpdateUtil.patchGeneric(tableName, updates, jdbcTemplate, dtoClass, getIdColumn(), id);
	}

	@Override
	public boolean softDelete(ID id) {
		String sql = "UPDATE " + tableName + " SET status = 'INACTIVE', updated_at = ?, updated_by = ? WHERE "
				+ getIdColumn() + " = ?";
		UUID currentUser = AppUtil.getCurrentUserId();
		return jdbcTemplate.update(sql, OffsetDateTime.now(), currentUser, id) > 0;
	}

	// --- helpers ---
	protected T mapSingle(Map<String, Object> result) {
		if (result == null || result.isEmpty())
			return null;
		try {
			T instance = dtoClass.getDeclaredConstructor().newInstance();
			result.forEach((k, v) -> {
				try {
					String fieldName = toCamel(k);
					var field = dtoClass.getDeclaredField(fieldName);
					field.setAccessible(true);
					field.set(instance, v);
				} catch (Exception ignored) {
				}
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
		if (name == null)
			return null;
		StringBuilder sb = new StringBuilder();
		boolean nextUpper = false;
		for (char c : name.toCharArray()) {
			if (c == '_') {
				nextUpper = true;
				continue; // important: skip appending underscore and don't reset flag here
			}
			if (nextUpper) {
				sb.append(Character.toUpperCase(c));
				nextUpper = false;
			} else {
				sb.append(c);
			}
		}
		return sb.toString();
	}

	/**
	 * Generic pagination method for all tables.
	 */
	public PagedResult<T> findAllPaginated(int page, int size, String stat) {
		String status = "ACTIVE"; // if (status == null || status.isEmpty()) status = "ACTIVE";
		page = Math.max(page, 0);
		size = size <= 0 ? 10 : size;
		int total = countByStatus(status);
		int totalPages = (int) Math.ceil((double) total / size);
		if (totalPages == 0) {
			page = 0; // no data at all
		} else if (page >= totalPages) {
			page = totalPages - 1; // clamp to last page
		}

		int offset = page * size;
		String sql = buildSelectWithLookups("t") + " WHERE t.status = ? ORDER BY t.updated_at DESC LIMIT ? OFFSET ?";
		List<T> results = jdbcTemplate.query(sql, new JsonAwareRowMapper<>(dtoClass), status, size, offset);
		return new PagedResult<>(results, page, size, total, (int) Math.ceil((double) total / size));
	}

	// --- HELPER: build SQL with dependent table JOINs ---

	protected String buildSelectWithLookups(String mainAlias) {
		if (dependentLookups == null || dependentLookups.isEmpty()) {
			return "SELECT " + mainAlias + ".* FROM " + tableName + " " + mainAlias;
		}

		StringBuilder sb = new StringBuilder("SELECT " + mainAlias + ".*");

		int joinIndex = 0;
		for (Map.Entry<String, DependentLookup> entry : dependentLookups.entrySet()) {
			DependentLookup lookup = entry.getValue();
			// Assuming your DependentLookup now has the 'resultAlias' field
			String alias = "j" + joinIndex;

			// Use lookup.resultAlias for the final SELECT alias
			sb.append(", ").append(alias).append(".").append(lookup.nameColumn).append(" AS ")
					.append(lookup.resultAlias); // <-- GENERIC CHANGE HERE
			joinIndex++;
		}

		sb.append(" FROM ").append(tableName).append(" ").append(mainAlias);

		joinIndex = 0;
		for (Map.Entry<String, DependentLookup> entry : dependentLookups.entrySet()) {
			String fkColumn = entry.getKey();
			DependentLookup lookup = entry.getValue();
			String alias = "j" + joinIndex;

			// JOIN structure remains the same
			sb.append(" LEFT JOIN ").append(lookup.tableName).append(" ").append(alias).append(" ON ").append(mainAlias)
					.append(".").append(fkColumn).append(" = ").append(alias).append(".").append(lookup.idColumn);
			joinIndex++;
		}
		return sb.toString();
	}

	protected String buildSelectWithLookups(String mainAlias, String some) {
		if (dependentLookups == null || dependentLookups.isEmpty()) {
			return "SELECT " + mainAlias + ".* FROM " + tableName + " " + mainAlias;
		}

		StringBuilder sb = new StringBuilder("SELECT " + mainAlias + ".*");

		int joinIndex = 0;
		for (Map.Entry<String, DependentLookup> entry : dependentLookups.entrySet()) {
			DependentLookup lookup = entry.getValue();
			String alias = "j" + joinIndex;

			sb.append(", ").append(alias).append(".").append(lookup.nameColumn).append(" AS ")
					.append(lookup.nameColumn);
			joinIndex++;
		}

		sb.append(" FROM ").append(tableName).append(" ").append(mainAlias);

		joinIndex = 0;
		for (Map.Entry<String, DependentLookup> entry : dependentLookups.entrySet()) {
			String fkColumn = entry.getKey();
			DependentLookup lookup = entry.getValue();
			String alias = "j" + joinIndex;

			sb.append(" LEFT JOIN ").append(lookup.tableName).append(" ").append(alias).append(" ON ").append(mainAlias)
					.append(".").append(fkColumn).append(" = ").append(alias).append(".").append(lookup.idColumn);
			joinIndex++;
		}
		return sb.toString();
	}

	public int countByStatus(String status) {
		String sql = "SELECT COUNT(*) FROM " + tableName + " WHERE status = ?";
		return jdbcTemplate.queryForObject(sql, Integer.class, status);
	}

	private String getStatus(String stat) {
		String status = null;
		if (stat == null || stat.isEmpty())
			status = "ACTIVE";
		return status;
	}

	/**
	 * Generic search across multiple fields like Google-style fuzzy search.
	 */
	public List<T> search(String searchText, List<String> searchFields, String stat) {
		String status = "ACTIVE"; // if (status == null || status.isEmpty()) status = "ACTIVE";

		if (stat != null && !stat.isBlank()) {
			status = stat;
		}
		if (searchText == null || searchText.isBlank() || searchFields == null || searchFields.isEmpty()) {
			List<T> all = findAll();
			return all;
		}
		StringBuilder whereClause = new StringBuilder(" WHERE UPPER(status) = ? AND (");
		List<Object> params = new ArrayList<>();
		params.add(status.toUpperCase());

		for (int i = 0; i < searchFields.size(); i++) {
//			whereClause.append(searchFields.get(i)).append(" ILIKE ?");
			whereClause.append("CAST(").append(searchFields.get(i)).append(" AS TEXT) ILIKE ?");
			params.add("%" + searchText + "%");
			if (i < searchFields.size() - 1)
				whereClause.append(" OR ");
		}
		whereClause.append(")");
		int limit = 10;
		String sql = "SELECT * FROM " + tableName + whereClause + " ORDER BY updated_at DESC LIMIT " + limit;
		return jdbcTemplate.query(sql, new JsonAwareRowMapper<>(dtoClass), params.toArray());
	}

	/**
	 * Generic search across multiple fields like Google-style fuzzy search.
	 */
	public PagedResult<T> searchPage(String searchText, List<String> searchFields, int page, int size, String stat) {
		String status = getStatus(stat); // if (status == null || status.isEmpty()) status = "ACTIVE";
		page = Math.max(page, 0);
		size = size <= 0 ? 10 : size;

		int offset = page * size;

		if (searchText == null || searchText.isBlank()) {
			return findAllPaginated(page, size, status);
		}

		StringBuilder whereClause = new StringBuilder(" WHERE status = ? AND (");
		List<Object> params = new ArrayList<>();
		params.add(status);

		for (int i = 0; i < searchFields.size(); i++) {
			whereClause.append(searchFields.get(i)).append(" ILIKE ?");
			params.add("%" + searchText + "%");
			if (i < searchFields.size() - 1)
				whereClause.append(" OR ");
		}
		whereClause.append(")");

		String sql = "SELECT * FROM " + tableName + whereClause + " ORDER BY updated_at DESC LIMIT ? OFFSET ?";
		String countSql = "SELECT COUNT(*) FROM " + tableName + whereClause;

		params.add(size);
		params.add(offset);

		List<T> results = jdbcTemplate.query(sql, new JsonAwareRowMapper<>(dtoClass), params.toArray());
		int total = jdbcTemplate.queryForObject(countSql, Integer.class,
				params.subList(0, params.size() - 2).toArray());

		return new PagedResult<>(results, page, size, total, (int) Math.ceil((double) total / size));
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

		return new PagedResult<>(results, page, size, total, (int) Math.ceil((double) total / size));
	}

	// ---------------------------
	// 1) Fetch metadata rows and parse extra_settings
	public List<DynamicFormMetaRow> getMetaRows() {
		String sql = "SELECT table_name, column_name, display_label, field_type, is_required, is_hidden, lookup_table, lookup_key, lookup_label, extra_settings, sort_order FROM form_metadata WHERE table_name = ? ORDER BY sort_order";
		return jdbcTemplate.query(sql, new JsonAwareRowMapper<>(DynamicFormMetaRow.class), this.tableName);
	}

	public Map<String, List<Map<String, Object>>> getLookupData(List<LookupDefinition> lookupDefs) {
		Map<String, List<Map<String, Object>>> lookupMap = new HashMap<>();

		for (LookupDefinition def : lookupDefs) {
			List<Map<String, Object>> data = loadLookupData(def.tableName(), def.keyColumn(), def.valueColumn());
			lookupMap.put(def.lookupKey(), data);
		}
		return lookupMap;
	}

	// 2) Load lookup rows for a lookup_table
	public List<Map<String, Object>> loadLookupData(String lookupTable, String keyColumn, String valueColumn) {
		String sql = String.format("SELECT %s AS key, %s AS value FROM %s ORDER BY %s", keyColumn, valueColumn,
				lookupTable, valueColumn);
		return jdbcTemplate.query(sql, (rs, rowNum) -> {
			Map<String, Object> m = new HashMap<>();
			m.put("key", rs.getObject("key"));
			m.put("value", rs.getObject("value"));
			return m;
		});
	}

	// 3) Build final DynamicFormResponseDto
	public DynamicFormResponseDto buildDynamicFormResponse() {

		List<DynamicFormMetaRow> meta = getMetaRows();
		List<DynamicFormMetaRow> newMeta = new ArrayList<>();

		for (DynamicFormMetaRow m : meta) {
			String apiField = toCamel(m.getColumnName());
			DynamicFormMetaRow row = new DynamicFormMetaRow(m.getTableName(), m.getColumnName(), m.getDisplayLabel(),
					m.getFieldType(), m.isRequired(), m.isHidden(), m.getLookupTable(), m.getLookupKey(),
					m.getLookupLabel(), m.getExtraSettings(), m.getSortOrder(), null, null);

			row.setApiField(apiField);
			newMeta.add(row);

			// If this column uses a lookup table → load dropdown data
			if (m.getLookupTable() != null && m.getLookupKey() != null && m.getLookupLabel() != null) {

				List<Map<String, Object>> lookupRows = loadLookupData(m.getLookupTable(), m.getLookupKey(),
						m.getLookupLabel());
				row.setLookupData(lookupRows);
			}
			if ("radio".equalsIgnoreCase(m.getFieldType()) && m.getLookupTable() == null && m.getLookupKey() != null
					&& !m.getLookupKey().isBlank()) {
				try {
					ObjectMapper mapper = new ObjectMapper();
					// parse JSON array string -> List<String>
					List<String> options = mapper.readValue(m.getLookupKey(), new TypeReference<List<String>>() {
					});
					// convert List<String> -> List<Map<String,Object>> with key/label
					List<Map<String, Object>> optionMaps = new ArrayList<>();
					for (String opt : options) {
						Map<String, Object> map = new HashMap<>();
						map.put("key", opt);
						map.put("label", opt);
						optionMaps.add(map);
					}
					row.setLookupData(optionMaps != null ? optionMaps : Collections.emptyList());
				} catch (Exception e) {
					row.setLookupData(Collections.emptyList());
				}
			}
		}
		String[] idArr = getIdColumn().split(",\\s*");
		return new DynamicFormResponseDto(this.tableName, idArr, newMeta);
	}
}
