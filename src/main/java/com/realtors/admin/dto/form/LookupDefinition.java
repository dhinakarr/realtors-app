package com.realtors.admin.dto.form;

public record LookupDefinition(
	    String lookupKey,     // e.g. "roles"
	    String tableName,     // e.g. "roles"
	    String keyColumn,     // e.g. "role_id"
	    String valueColumn,    // e.g. "role_name"
	    String resultAlias
	) {}
