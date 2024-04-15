package io.mosip.registration.processor.data.verification.constants;

import java.util.regex.Pattern;

public class DataVerificationConstants {

    /** The Constant VER. */
    public static final String VER = "version";

    /** The Constant verPattern. */
    public static final Pattern verPattern = Pattern.compile("^[0-9](\\.\\d{1,1})?$");

    /** The Constant DATETIME_TIMEZONE. */
    public static final String DATETIME_TIMEZONE = "mosip.registration.processor.timezone";

    /** The Constant DATETIME_PATTERN. */
    public static final String DATETIME_PATTERN = "mosip.registration.processor.datetime.pattern";

    /** The Constant ID_REPO_SERVICE. */
    public static final String DAT_VERI_SERVICE = "DataVerificationService";
    public static final String ASSIGNMENT_SERVICE_ID = "mosip.registration.processor.manual.verification.assignment.id";
    public static final String DECISION_SERVICE_ID = "mosip.registration.processor.manual.verification.decision.id";
    public static final String BIOMETRIC_SERVICE_ID = "mosip.registration.processor.manual.verification.biometric.id";
    public static final String DEMOGRAPHIC_SERVICE_ID = "mosip.registration.processor.manual.verification.demographic.id";
    public static final String PACKETINFO_SERVICE_ID = "mosip.registration.processor.manual.verification.packetinfo.id";
    public static final String MVS_APPLICATION_VERSION = "mosip.registration.processor.application.version";
    public static final String VERIFICATION_APPROVED = "Manual verification approved for registration id : ";
    public static final String VERIFICATION_REJECTED = "Manual verification rejected for registration id : ";
    public static final String TABLE_NOT_ACCESSIBLE = "Table notAccessibleException in Dafta verification for registrationId: ";
    public static final String USERS = "users";
    public static final String TIME_FORMAT = "yyyy-MM-dd'T'hh:mm:ss.000'Z'";
    public static final String ACT ="ACT";
    public static final String DATA_VERIFICATION_ID = "technify.data.verification.verify";
    public static final String VERSION="1.0";
    public static final String POLICY_ID = "policyId";
    public static final String POLICIES = "policies";
    public static final String SHAREABLE_ATTRIBUTES = "shareableAttributes";
}
