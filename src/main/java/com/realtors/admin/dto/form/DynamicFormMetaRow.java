package com.realtors.admin.dto.form;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DynamicFormMetaRow {
    private String tableName;
    private String columnName;
    private String displayLabel;
    private String fieldType;
    private boolean isRequired;
    private boolean isHidden;
    private String lookupTable;
    private String lookupKey;
    private String lookupLabel;
    private Map<String,Object> extraSettings;  
    private Integer sortOrder;
    private List<Map<String,Object>> lookupData;
    private String apiField;
}
