package io.mosip.registration.processor.data.verification.dto;


import lombok.Data;

import java.util.List;

@Data
public class DataVerificationPolicyDTO {
    private String policyName;
    private String description;

    private String organizationName;
    private String version;
    private String maxPerDay;
    private List<String> center;
    private String [] machine;
    private String [] officer;
    private String [] nationality;
    private Object ageRange;

}
