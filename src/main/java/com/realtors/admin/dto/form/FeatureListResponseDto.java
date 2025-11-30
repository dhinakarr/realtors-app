package com.realtors.admin.dto.form;

import java.util.List;
import java.util.Map;

import com.realtors.admin.dto.PagedResult;

public record FeatureListResponseDto<T>(
        String title,
        String layout, // table, card
        List<String> displayColumns,
        Map<String, String> fieldMap,
        String rowKeyField,
        boolean paginated,
        PagedResult<T> pageData,
        Map<String, List<Map<String, Object>>> lookup
) {}
