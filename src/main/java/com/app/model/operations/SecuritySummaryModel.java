package com.app.model.operations;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SecuritySummaryModel {
    private String securityId;
    private String authPolicy;
    private boolean encryptionAtRest;
    private boolean encryptionInTransit;
    private String secretRotationStatus;
}
