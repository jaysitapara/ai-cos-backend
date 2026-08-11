package com.app.model.architecture;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiEndpointSpec {
    private String httpMethod;
    private String path;
    private String summary;
    private Map<String, Object> requestBody;
    private Map<String, Object> responseBody;
    private boolean authRequired;
}
