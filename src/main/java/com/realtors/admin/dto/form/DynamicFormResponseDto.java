package com.realtors.admin.dto.form;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DynamicFormResponseDto {
    private String tableName;
    private String[] primaryField;
    private List<DynamicFormMetaRow> fields;
}
