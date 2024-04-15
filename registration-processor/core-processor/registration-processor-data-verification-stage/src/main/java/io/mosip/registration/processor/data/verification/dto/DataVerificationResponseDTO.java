package io.mosip.registration.processor.data.verification.dto;

import lombok.Data;

@Data
public class DataVerificationResponseDTO {


    private String requestId;

    private String decision;

    private String reason;

    private String comment;

    private String responseDate;

    private String referenceId;
}
