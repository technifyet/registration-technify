package io.mosip.registration.processor.data.verification.dto;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

import io.mosip.registration.processor.data.verification.constants.DataVerificationConstants;
import lombok.Data;

@Data
public class DataVerificationRequestDTO {

    private String id;

    private String version;

    private String requestId;

    private String referenceId;

    private String requesttime;

    private String referenceURL;

}
