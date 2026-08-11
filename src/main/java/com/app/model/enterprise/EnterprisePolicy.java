package com.app.model.enterprise;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnterprisePolicy {
    private String policyId;
    private String orgId;
    private List<String> aiModelRestrictions;
    private int dataRetentionDays;
    private boolean rbacEnforced;
    private boolean auditLoggingStrict;
}
