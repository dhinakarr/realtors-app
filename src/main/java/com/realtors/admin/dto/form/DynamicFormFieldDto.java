package com.realtors.admin.dto.form;

import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DynamicFormFieldDto {
    private String columnName;
    private String label;
    private String inputType;
    private boolean required;
    private boolean hidden;
    private List<Map<String,Object>> lookupData; // optional dropdown values
    private Map<String,Object> extraSettings; // parsed JSONB -> Map
}
