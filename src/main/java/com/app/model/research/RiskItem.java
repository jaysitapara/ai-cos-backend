package com.app.model.research;

import com.app.enums.RiskCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiskItem {
    private String riskId;
    private String title;
    private RiskCategory category;
    private String severity; // HIGH, MEDIUM, LOW
    private String description;
    private String mitigation;
}
