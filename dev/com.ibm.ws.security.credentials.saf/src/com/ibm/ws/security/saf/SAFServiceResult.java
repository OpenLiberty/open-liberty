/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.saf;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ffdc.FFDCFilter;

/**
 * SAFServiceResult encapsulates the return and reason codes of a native
 * SAF service invocation.
 *
 * IRRSAI00 return/reason codes:
 * http://publib.boulder.ibm.com/infocenter/zos/v1r12/index.jsp?topic=%2Fcom.ibm.zos.r12.ichd100%2Fichzd1b0109.htm
 *
 * RACROUTE REQUEST=AUTH return/reason codes:
 * http://publib.boulder.ibm.com/infocenter/zos/v1r12/index.jsp?topic=%2Fcom.ibm.zos.r12.ichc600%2Fichzc6b024.htm
 */
public class SAFServiceResult {

    /**
     * TraceComponent for issuing messages.
     */
    private static final TraceComponent tc = Tr.register(SAFServiceResult.class);

    /**
     * Length of the byte array containing SAF service return and reason codes and SAF service code
     */
    private final static int SAF_RETURN_AREA_LENGTH = 20; // 5 ints

    /**
     * Any wasReturnCodes from the server's penalty box contain the following value
     * in its 5 hi-order nibbles. The 3 lo-order nibbles contain the specific RC from
     * the penalty box.
     *
     * Note: The mask is the same as the RAS_MODULE_CONST for security_saf_sandbox.mc,
     * defined in ras_tracing.h. The RAS_MODULE_CONST and this value must be kept in sync.
     */
    private static final int PENALTY_BOX_RC_MASK = 0x03008000;

    /**
     * Penalty box RC filter for APPL failures.
     * Note: This value must be kept in sync with value in security_saf_sandbox.mc.
     */
    private static final int PENALTY_BOX_APPL_ERROR_MASK = 0x100;

    /**
     * Penalty box RC filter for EJBROLE PROFILE HLQ failures.
     * Note: This value must be kept in sync with value in security_saf_sandbox.mc.
     */
    private static final int PENALTY_BOX_PROFILE_ERROR_MASK = 0x200;

    /**
     * Penalty box RC filter for CLASS failures.
     * Note: This value must be kept in sync with value in security_saf_sandbox.mc.
     */
    private static final int PENALTY_BOX_CLASS_ERROR_MASK = 0x400;

    /**
     * WAS return code when element was freed or a free is pending, so we can't use it
     *
     * Note: This value is defined in server_util_registry.mc and must be kept in sync.
     */
    private static final int UTIL_REGISTRY_RC_PENDING_FREE = 0x02008002;

    /**
     * Indicates that the SAFServiceResult contains no error.
     */
    private static final int NO_ERROR = 0;

    /**
     * Indicates that the SAFServiceResult contains a severe or unknown error.
     * These kinds of errors we should raise exceptions or cut FFDC records for.
     */
    private static final int SEVERE_ERROR = 1;

    /**
     * Indicates that the SAFServiceResult contains an unexpected error.
     * These kinds of errors we should log.
     */
    private static final int UNEXPECTED_ERROR = 2;

    /**
     * Indicates that the SAFServiceResult contains an expected error.
     * These kinds of errors we should not make too much of a fuss about.
     */
    private static final int EXPECTED_ERROR = 3;

    /**
     * This byte array is passed to native and populated by the native code.
     */
    private final byte[] safResultBytes = new byte[SAF_RETURN_AREA_LENGTH];

    /**
     * SAF service return and reason codes
     */
    private int wasReturnCode;
    private int safReturnCode;
    private int racfReturnCode;
    private int racfReasonCode;

    /**
     * Type of SAF service invoked
     */
    private SAFService safService;

    /**
     * Boolean indicates whether we've already parsed the error codes from the raw
     * byte[] that gets filled in by the native code.
     */
    private boolean areErrorCodesSet = false;

    /**
     * The userSecurityName associated with the SAF request that generated this SAFServiceResult.
     */
    private String userSecurityName = null;

    /**
     * The APPL-ID associated with the SAF request that generated this SAFServiceResult.
     */
    private String applId = null;

    /**
     * The SAF resource profile name associated with the SAF request that generated this SAFServiceResult.
     */
    private String safProfile = null;

    /**
     * The SAF CLASS associated with the SAF request that generated this SAFServiceResult.
     */
    private String safClass = null;

    /**
     * The SAF CLASS associated with the SAF request that generated this SAFServiceResult.
     */
    private String groupName = null;

    /**
     * The nlsprops msgKey associated with this SAFServiceResult.
     */
    private String msgKey = null;

    /**
     * The nlsprops msg fill-ins assocated with the msgKey associated with this SAFServiceResult.
     */
    private Object[] msgFillIns = null;

    /**
     * Volume Serial Number of volume where dataset located. Used for isAuthorizedToDataset
     * and isGroupAuthorizedToDataset
     */
    private String volser = null;

    /**
     * Flag to specify if dataset is vsam or non-vsam. Used for isAuthorizedToDataset and isGroupAuthorizedToDataset
     */
    private boolean vsam = false;

    /**
     * Enum to associate the type of SAF service invoked
     */
    @Trivial
    public enum SAFService {
        // serviceCode == ordinal().
        // This list must be kept consistent with the SAFService enum defined in
        // com.ibm.zos.native/include/security_saf_common.h.
        WAS_INTERNAL,
        IRRSIA00_CREATE,
        IRRSIA00_DELETE,
        IRRSIA00_PURGE,
        IRRSIA00_REGISTER,
        IRRSIA00_DEREGISTER,
        IRRSIA00_QUERY,
        RACROUTE_AUTH,
        RACROUTE_EXTRACT,
        RACROUTE_FASTAUTH,
        BPX4TLS_DELETE_THREAD_SEC,
        BPX4TLS_TASK_ACEE,
        RACROUTE_VERIFY;

        public final int serviceCode;

        SAFService() {
            this.serviceCode = ordinal();
        }

        public int getServiceCode() {
            return serviceCode;
        }

        public static SAFService forServiceCode(int x) {
            final SAFService[] enums = values();
            if (x >= 0 && x < enums.length) {
                return enums[x];
            }
            return null;
        }
    }

    /**
     * CTOR.
     */
    public SAFServiceResult() {
    }

    /**
     * Return the byte[] area that contains the return/reason codes.
     */
    public byte[] getBytes() {
        return safResultBytes;
    }

    /**
     * Save the return/reason codes.
     */
    private void setErrorCodes() {
        if (!areErrorCodesSet) {
            IntBuffer ibuff = ByteBuffer.wrap(getBytes()).asIntBuffer();

            wasReturnCode = ibuff.get();
            safReturnCode = ibuff.get();
            racfReturnCode = ibuff.get();
            racfReasonCode = ibuff.get();
            safService = SAFService.forServiceCode(ibuff.get());

            areErrorCodesSet = true;
        }
    }

    /**
     * @return the was internal return code.
     */
    public int getWasReturnCode() {
        setErrorCodes();
        return wasReturnCode;
    }

    /**
     * @return the SAF return code.
     */
    public int getSAFReturnCode() {
        setErrorCodes();
        return safReturnCode;
    }

    /**
     * @return the RACF return code.
     */
    public int getRacfReturnCode() {
        setErrorCodes();
        return racfReturnCode;
    }

    /**
     * @return the RACF reason code.
     */
    public int getRacfReasonCode() {
        setErrorCodes();
        return racfReasonCode;
    }

    public String getUserSecurityName() {
        return userSecurityName;
    }

    public String getApplID() {
        return applId;
    }

    public String getSafProfile() {
        return safProfile;
    }

    public String getSafClass() {
        return safClass;
    }

    public String getVolser() {
        return volser;
    }

    public boolean getVsam() {
        return vsam;
    }

    public String getGroupSecurityName() {
        return groupName;
    }

    /**
     * @return the SAF service.
     */
    public SAFService getSAFService() {
        setErrorCodes();
        return safService;
    }

    /**
     * Throw an exception based on the SAF service failure.
     */
    public void throwSAFException() throws SAFException {
        throw getSAFException();
    }

    /**
     * @return a SAFException representing this SAFServiceResult
     */
    public SAFException getSAFException() {
        setErrorCodes();
        return new SAFException(this);
    }

    /**
     * Determine the severity of the error contained in this SAFServiceResult.
     *
     * @return 0: No error.
     *         1: High severity and unknown error.
     *         2: Unexpected error (e.g penalty box errors, or profile-doesn't-exist errors).
     *         3: Expected error (e.g bad password).
     */
    private int getSeverity() {
        setErrorCodes();

        if (wasReturnCode == 0 && safReturnCode == 0) {
            return NO_ERROR;
        }

        switch (safService) {
            case WAS_INTERNAL:
                if ((wasReturnCode & 0xFFFFF000) == PENALTY_BOX_RC_MASK) {
                    return UNEXPECTED_ERROR;
                }
                break;

            case IRRSIA00_CREATE:
                if ((safReturnCode == 8) && (racfReturnCode == 8)) {
                    switch (racfReasonCode) {
                        case 16: // UserId not defined.
                        case 20: // Bad password.
                        case 24: // Bad passphrase.
                        case 28: // UserId revoked.
                            return EXPECTED_ERROR;
                        case 32: // No access to appl-id.
                            return UNEXPECTED_ERROR;
                    }
                }
                break;

            case RACROUTE_AUTH:
            case RACROUTE_FASTAUTH:
                // 4/4/x: PROFILE doesn't exist or CLASS isn't active
                // NOTE: for FASTAUTH, 4/4/x could also mean the CLASS isn't RACLISTed.
                // However, the native code detects this result and automatically
                // tries regular auth. So if we get a 4/4/x back to Java, it probably means the
                // PROFILE doesn't exist.
                if (safReturnCode == 4 && racfReturnCode == 4) {
                    return UNEXPECTED_ERROR;
                }
                // 8/8/0: user is NOT authorized to the resource PROFILE.
                if (safReturnCode == 8 && racfReturnCode == 8) {
                    return EXPECTED_ERROR;
                }
                break;
        }
        return SEVERE_ERROR;
    }

    /**
     * Convenience method for converting an int to a hex string.
     */
    private static String hexIt(int z) {
        return String.format("0x%08x", Integer.valueOf(z));
    }

    /**
     * Set the msgKey and msgFillIns according to the SAF results.
     */
    private void setMessage() {
        if (msgKey != null) {
            return; // The msgKey and msgFillIns have already been set.
        }

        setErrorCodes();

        switch (safService) {
            case WAS_INTERNAL:
                if ((wasReturnCode & 0xFFFFF000) == PENALTY_BOX_RC_MASK) {
                    String failedResource = null;
                    int pbRCMask = (wasReturnCode & 0x00000F00);
                    switch (pbRCMask) {
                        case PENALTY_BOX_APPL_ERROR_MASK:
                            failedResource = "APPL-ID " + applId;
                            break;
                        case PENALTY_BOX_PROFILE_ERROR_MASK:
                            failedResource = "EJBROLE PROFILE " + safProfile;
                            break;
                        case PENALTY_BOX_CLASS_ERROR_MASK:
                            failedResource = "CLASS " + safClass;
                            break;
                        default:
                            failedResource = "Indeterminate. One of APPL-ID " + applId + ", EJBROLE PROFILE " + safProfile + ", or CLASS " + safClass;
                            break;
                    }
                    msgKey = "PENALTY_BOX_ERROR";
                    msgFillIns = new Object[] { failedResource, hexIt(wasReturnCode) };
                }

                break;

            case IRRSIA00_CREATE:
                if ((safReturnCode == 4) && (racfReturnCode == 0) && (racfReasonCode == 0)) {
                    msgKey = "SAF_RACF_NOT_INSTALLED_ERROR";
                    msgFillIns = new Object[] { safService, hexIt(safReturnCode), hexIt(racfReturnCode), hexIt(racfReasonCode) };

                } else if ((safReturnCode == 8) && (racfReturnCode == 8)) {
                    switch (racfReasonCode) {
                        case 4:
                            msgKey = "SAF_PARAMETER_LIST_ERROR";
                            msgFillIns = new Object[] { safService, hexIt(safReturnCode), hexIt(racfReturnCode), hexIt(racfReasonCode) };
                            break;
                        case 8:
                            msgKey = "SAF_INTERNAL_ERROR";
                            msgFillIns = new Object[] { safService, hexIt(safReturnCode), hexIt(racfReturnCode), hexIt(racfReasonCode) };
                            break;
                        case 12:
                            msgKey = "SAF_RECOVERY_ERROR";
                            msgFillIns = new Object[] { safService, hexIt(safReturnCode), hexIt(racfReturnCode), hexIt(racfReasonCode) };
                            break;
                        case 16:
                            if (userSecurityName != null) {
                                msgKey = "SAF_USERID_NOT_DEFINED_ERROR";
                                msgFillIns = new Object[] { safService, hexIt(safReturnCode), hexIt(racfReturnCode), hexIt(racfReasonCode), userSecurityName };
                                break;
                            } else {
                                msgKey = "SAF_GROUPID_NOT_DEFINED_ERROR";
                                msgFillIns = new Object[] { safService, hexIt(safReturnCode), hexIt(racfReturnCode), hexIt(racfReasonCode), groupName };
                                break;
                            }

                        case 28:
                            if (userSecurityName != null) {
                                msgKey = "SAF_USERID_REVOKED_ERROR";
                                msgFillIns = new Object[] { safService, hexIt(safReturnCode), hexIt(racfReturnCode), hexIt(racfReasonCode), userSecurityName };
                                break;
                            } else {
                                msgKey = "SAF_GROUPID_REVOKED_ERROR";
                                msgFillIns = new Object[] { safService, hexIt(safReturnCode), hexIt(racfReturnCode), hexIt(racfReasonCode), groupName };
                                break;
                            }
                        case 32:
                            if (userSecurityName != null) {
                                msgKey = "SAF_INVALID_ACCESS_ERROR";
                                msgFillIns = new Object[] { safService, hexIt(safReturnCode), hexIt(racfReturnCode), hexIt(racfReasonCode), userSecurityName, applId };
                                break;
                            } else {
                                msgKey = "SAF_INVALID_GROUP_ACCESS_ERROR";
                                msgFillIns = new Object[] { safService, hexIt(safReturnCode), hexIt(racfReturnCode), hexIt(racfReasonCode), groupName, applId };
                                break;
                            }
                        default:
                            break;
                    }
                }
                break;

            case RACROUTE_AUTH:
            case RACROUTE_FASTAUTH:
                if (safReturnCode == 4 && racfReturnCode == 4) {
                    msgKey = "SAF_PROFILE_NOT_DEFINED";
                    msgFillIns = new Object[] { safService, hexIt(safReturnCode), hexIt(racfReturnCode), hexIt(racfReasonCode), safProfile, safClass };
                } else if (safReturnCode == 8 && racfReturnCode == 8) {
                    if (userSecurityName != null) {
                        msgKey = "SAF_USER_NOT_AUTHORZED_TO_RESOURCE";
                        msgFillIns = new Object[] { safService, hexIt(safReturnCode), hexIt(racfReturnCode), hexIt(racfReasonCode), userSecurityName, safProfile, safClass };
                    } else {
                        msgKey = "SAF_GROUP_NOT_AUTHORZED_TO_RESOURCE";
                        msgFillIns = new Object[] { safService, hexIt(safReturnCode), hexIt(racfReturnCode), hexIt(racfReasonCode), groupName, safProfile, safClass };
                    }
                }
                break;
            case BPX4TLS_DELETE_THREAD_SEC:
            case BPX4TLS_TASK_ACEE:
                msgKey = "UNIX_SYSTEM_SERVICE_ERROR";
                msgFillIns = new Object[] { safService, hexIt(safReturnCode), hexIt(racfReturnCode), hexIt(racfReasonCode), hexIt(wasReturnCode) };
                break;
            default:
                break;
        }

        // If no match, use default unknown error message.
        if (msgKey == null) {
            msgKey = "SAF_UNKNOWN_ERROR";
            msgFillIns = new Object[] { safService, hexIt(safReturnCode), hexIt(racfReturnCode), hexIt(racfReasonCode), hexIt(wasReturnCode) };
        }
    }

    /**
     * Translate exception message based on the SAF routine, return codes and reason codes.
     */
    public String getMessage() {
        setErrorCodes();

        setMessage();

        return Tr.formatMessage(tc, msgKey, msgFillIns);
    }

    /**
     * Determine if the SAF service failure is SEVERE.
     *
     * @return true if the SAF service failure is severe;
     *         false otherwise.
     */
    public boolean isSevere() {
        return (getSeverity() == SEVERE_ERROR);
    }

    /**
     * Determine if the SAF service failure is a Penalty box error.
     *
     * @return true if the SAF service failure is a Penalty box error
     *         false otherwise.
     */
    public boolean isPenaltyBoxError() {

        setErrorCodes();
        boolean rc = false;

        if (wasReturnCode == 0 && safReturnCode == 0) {
            return rc;
        }

        if (safService == SAFService.WAS_INTERNAL)
            if ((wasReturnCode & 0xFFFFF000) == PENALTY_BOX_RC_MASK)
                rc = true;

        return rc;

    }

    /**
     * Determine if the SAF service failure is a PasswordExpired error.
     *
     * @return true if the safReturnCode = 8, racfReturnCdoe = 8, and racfReasonCode = 24 and
     *         false if anything else.
     *
     */
    public boolean isPasswordExpiredError() {
        setErrorCodes();
        boolean rc = false;

        if (wasReturnCode == 0 && safReturnCode == 0) {
            return rc;
        }
        if (safReturnCode == 8 && racfReturnCode == 8 && racfReasonCode == 24) {
            rc = true;
        }
        return rc;
    }

    /**
     * Determine if the SAF service failure is a UserRevoked error.
     *
     * @return true if safReturnCdoe = 8, racfReturnCode = 8, and racfReasonCode = 28, and
     *         false if anything else.
     */
    public boolean isUserRevokedError() {
        setErrorCodes();
        boolean rc = false;

        if (wasReturnCode == 0 && safReturnCode == 0) {
            return rc;
        }
        if (safReturnCode == 8 && racfReturnCode == 8 && racfReasonCode == 28) {
            rc = true;
        }
        return rc;
    }

    /**
     * Determine if the SAF service failure is unexpected.
     * This includes severe errors.
     *
     * @return true if the SAF service failure is unexpected or severe;
     *         false otherwise.
     */
    public boolean isUnexpected() {
        return (getSeverity() == UNEXPECTED_ERROR || isSevere());
    }

    /**
     * Log unexpected failures.
     *
     * If SEVERE_ERROR, cut an FFDC record.
     * If UNEXPECTED_ERROR, log a message.
     * If EXPECTED_ERROR, do nothing.
     *
     * @param logObjects Objects to include in the log record.
     */
    public void logIfUnexpected() {
        int sev = getSeverity();
        if (sev == SEVERE_ERROR) {
            FFDCFilter.processException(new SAFException(this), SAFServiceResult.class.getName(), "1", this);
        } else if (sev == UNEXPECTED_ERROR) {
            setMessage();
            Tr.error(tc, msgKey, msgFillIns);
        }
    }

    /**
     * Set some private fields.
     *
     * @param userSecurityName
     * @param applId
     * @param safProfile
     * @param safClass
     */
    public void setFields(String userSecurityName, String applId, String safProfile, String safClass, String groupName, String volser, boolean vsam) {
        this.userSecurityName = userSecurityName;
        this.applId = applId;
        this.safProfile = safProfile;
        this.safClass = safClass;
        this.groupName = groupName;
        this.volser = volser;
        this.vsam = vsam;
    }

    public void setSafClass(String className) {
        this.safClass = className;
    }

    public void setSafProfile(String safProfile) {
        this.safProfile = safProfile;
    }

    /**
     * Set some private fields.
     *
     * @param userSecurityName
     * @param applId
     */
    public void setAuthenticationFields(String userSecurityName, String applId) {
        setFields(userSecurityName, applId, null, null, null, null, false);
    }

    /**
     * Set some private fields.
     *
     * @param safProfile
     * @param safClass
     * @param applId
     */
    public void setAuthorizationFields(String userSecurityName, String safProfile, String safClass, String applId, String volser, boolean vsam) {
        setFields(userSecurityName, applId, safProfile, safClass, null, volser, vsam);
    }

    public void setGroupAuthorizationFields(String groupName, String safProfile, String safClass, String applId, String volser, boolean vsam) {
        setFields(null, applId, safProfile, safClass, groupName, volser, vsam);
    }

    /**
     * Check if the WAS return code indicates that getting the SAF cred token failed
     * because another thread was deleting it, and the operation can be retried
     *
     * @return true if the operation is retryable, otherwise false
     */
    public boolean isRetryable() {
        setErrorCodes();
        return SAFServiceResult.UTIL_REGISTRY_RC_PENDING_FREE == wasReturnCode;
    }

    /**
     * Call yield and return true, so it can be used in a boolean context.
     */
    public static boolean yield() {
        Thread.yield();
        return true;
    }

    @Override
    public String toString() {
        return "wasReturnCode: " + wasReturnCode + " safReturnCode: " + safReturnCode + " racfReturnCode: " + racfReturnCode
               + " racfReasonCode: " + racfReasonCode + " userSecurityName: " + userSecurityName + " applid: " + applId
               + " safProfile: " + safProfile + " safClass: " + safClass + " volser: " + volser + " vsam: " + vsam;
    }
}
