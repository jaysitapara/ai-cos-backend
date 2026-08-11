package com.app.model.architecture;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EntitySchema {
    private String entityName;
    private String tableName;
    private List<Map<String, Object>> fields;
    private List<Map<String, Object>> relationships;
}
