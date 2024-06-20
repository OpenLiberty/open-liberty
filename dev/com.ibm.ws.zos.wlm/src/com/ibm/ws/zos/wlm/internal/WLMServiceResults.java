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
package com.ibm.ws.zos.wlm.internal;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.websphere.ras.annotation.Trivial;

/**
 *
 */
public class WLMServiceResults {
    /**
     * TraceComponent for this class.
     */
    private final static TraceComponent tc = Tr.register(WLMServiceResults.class);

    /**
     * Thread level data to hold results of WLM calls.
     */
    private final static class WLMResultsThreadLocal extends ThreadLocal<WLMServiceResults> {
        @Override
        @Sensitive
        protected WLMServiceResults initialValue() {
            return new WLMServiceResults();
        }

        @Override
        public String toString() {
            return get().toString();
        }
    }

    /**
     * This object holds the results of the last WLM call that was made in
     * native code. It's intended to be similar to the concept of errno and
     * errno2 in Unix and C libraries. <br>
     * If you get a bad return code from one of the services, you can look at
     * this object for more information on the service that was called and its
     * return codes.
     */
    private final static ThreadLocal<WLMServiceResults> results = new WLMResultsThreadLocal();

    /**
     * WLM return code.
     */
    private int _returnCode;

    /**
     * WLM reason code.
     */
    private int _reasonCode;

    /**
     * Service that generated the return codes.
     */
    private String _serviceName;

    /**
     * WebSphere service return code.
     */
    private int _wasReturnCode;

    /**
     * Special failure WebSphere service return codes. Much match values in native
     * part server_wlm_services.h
     */
    static final protected int WASRETURNCODE_FAILED_TO_REGISTER = -100;
    static final protected int WASRETURNCODE_FAILED_TO_VALIDATE_IN_REGISTRY = -101;
    static final protected int WASRETURNCODE_FAILED_TO_FIND_UNAUTH_FUNCTION_STUBS = -102;
    static final protected int WASRETURNCODE_FAILED_CALLING_BPX4IPT = -103;

    /**
     * WebSphere callable service name.
     */
    private String _wasServiceName;

    /**
     * Return Data from the service call
     */
    private byte[] _returnData;

    /**
     * Default constructor.
     */
    @Trivial
    protected WLMServiceResults() {
    }

    /**
     * Set the WLM codes and service name.
     * <p>
     * This method is called by the native code
     */
    @SuppressWarnings("unused")
    protected static void setResults(int returnCode,
                                     int reasonCode,
                                     String serviceName,
                                     int wasRetCode,
                                     String wasService,
                                     byte[] returnData) {

        WLMServiceResults result = getWLMServiceResult();

        result._returnCode = returnCode;
        result._reasonCode = reasonCode;
        result._serviceName = serviceName != null ? serviceName.trim() : null;
        result._wasReturnCode = wasRetCode;
        result._wasServiceName = wasService != null ? wasService.trim() : null;
        result._returnData = returnData;
    }

    /**
     * Issue a message based on the current results
     */
    @Sensitive
    public void issueWLMServiceMessage() {

        int wasRetCode = _wasReturnCode;
        int returnCode = _returnCode;
        int reasonCode = _reasonCode;

        if ((_serviceName == null) && (_wasServiceName == null))
            return;

        String serviceName = String.valueOf(_serviceName);
        String wasServiceName = String.valueOf(_wasServiceName);

        if (wasServiceName.equals(WLMServiceResults.UNAUTH_CreateWorkUnit)) {
            if ((returnCode == 0) &&
                (ERRNO.ENOMEM.errno() == wasRetCode)) {
                // Native code run into NO MEMORY condition
                Tr.audit(tc,
                         "NATIVE_SERVICE_OOM",
                         new Object[] { wasServiceName });
            } else if ((ERRNO.EPERM.errno() == returnCode) ||
                       (ERRNO.EMVSSAF2ERR.errno() == returnCode)) {
                // If its a security reason to throw
                throw new SecurityException("WLM enclave operation, " + serviceName + ", disallowed by security product, errno (" + (Integer.valueOf(returnCode)).toString()
                                            + ").");
            } else if (ERRNO.EMVSWLMERROR.errno() == returnCode) {
                // WLM failed the service call
                Tr.audit(tc,
                         "NATIVE_SERVICE_WLM_FAILURE",
                         new Object[] { serviceName, reasonCode });
            } else {
                // Some other reason??
                Tr.audit(tc,
                         "NATIVE_SERVICE_ERRNO",
                         new Object[] { serviceName, returnCode });
            }
        } else if (wasServiceName.equals(WLMServiceResults.UNAUTH_JoinWorkUnit)) {
            if (ERRNO.EMVSWLMERROR.errno() == returnCode) {
                if ((WLMReasonCodes.IWMRSNCODEBEGINENVOUTSTANDING.rsnCode() == reasonCode) ||
                    (WLMReasonCodes.IWMRSNCODEALREADYINENCLAVE.rsnCode() == reasonCode)) {
                    // Skip issuing a message under the case where the WLM Enclave is already joined
                } else {
                    // WLM failed the service call
                    Tr.audit(tc,
                             "NATIVE_SERVICE_WLM_FAILURE",
                             new Object[] { serviceName, reasonCode });
                }
            } else {
                // Caller is throwing for non-successful cases.
            }
        } else if (wasServiceName.equals(WLMServiceResults.UNAUTH_CreateJoinWorkUnit)) {
            if ((returnCode == 0) &&
                (ERRNO.ENOMEM.errno() == wasRetCode)) {
                // Native code run into NO MEMORY condition
                Tr.audit(tc,
                         "NATIVE_SERVICE_OOM",
                         new Object[] { wasServiceName });
            } else if ((ERRNO.EPERM.errno() == returnCode) ||
                       (ERRNO.EMVSSAF2ERR.errno() == returnCode)) {
                // If its a security reason to throw
                throw new SecurityException("WLM enclave operation, " + serviceName + ", disallowed by security product, errno (" + (Integer.valueOf(returnCode)).toString()
                                            + ").");
            } else if (ERRNO.EMVSWLMERROR.errno() == returnCode) {
                if ((WLMReasonCodes.IWMRSNCODEBEGINENVOUTSTANDING.rsnCode() == reasonCode) ||
                    (WLMReasonCodes.IWMRSNCODEALREADYINENCLAVE.rsnCode() == reasonCode)) {
                    // Skip issuing a message under the case where the WLM Enclave is already joined
                } else {
                    // WLM failed the service call
                    Tr.audit(tc,
                             "NATIVE_SERVICE_WLM_FAILURE",
                             new Object[] { serviceName, reasonCode });
                }
            } else {
                // Some other reason??
                Tr.audit(tc,
                         "NATIVE_SERVICE_ERRNO",
                         new Object[] { serviceName, returnCode });
            }
        } else if (wasServiceName.equals(WLMServiceResults.UNAUTH_LeaveWorkUnit) ||
                   wasServiceName.equals(WLMServiceResults.UNAUTH_DeleteWorkUnit) ||
                   wasServiceName.equals(WLMServiceResults.UNAUTH_LeaveDeleteWorkUnit)) {
            if ((ERRNO.EPERM.errno() == returnCode) ||
                (ERRNO.EMVSSAF2ERR.errno() == returnCode)) {
                // If its a security reason to throw
                throw new SecurityException("WLM enclave operation, " + serviceName + ", disallowed by security product, errno (" + (Integer.valueOf(returnCode)).toString()
                                            + ").");

            } else if (ERRNO.EMVSWLMERROR.errno() == returnCode) {
                // WLM failed the service call
                Tr.audit(tc,
                         "NATIVE_SERVICE_WLM_FAILURE",
                         new Object[] { serviceName, reasonCode });
            }
        } else if (wasServiceName.equals(WLMServiceResults.UNAUTH_ConnectAsWorkMgr)) {
            if ((ERRNO.EPERM.errno() == returnCode) ||
                (ERRNO.EMVSSAF2ERR.errno() == returnCode)) {
                // If its a security reason to throw
                throw new SecurityException("WLM enclave operation, " + serviceName + ", disallowed by security product, errno (" + (Integer.valueOf(returnCode)).toString()
                                            + ").");

            } else if (ERRNO.EMVSWLMERROR.errno() == returnCode) {
                // WLM failed the service call
                Tr.audit(tc,
                         "NATIVE_SERVICE_WLM_FAILURE",
                         new Object[] { serviceName, reasonCode });
            }
        } else if (wasServiceName.equals(WLMServiceResults.AUTH_ConnectAsWorkMgr)) {
            if (wasRetCode == WASRETURNCODE_FAILED_TO_FIND_UNAUTH_FUNCTION_STUBS) {
                // Native code failed to register new WLM Enclave into native registry
                Tr.audit(tc,
                         "NATIVE_SERVICE_UNAUTH_SERVICE_NOTFOUND",
                         new Object[] { wasServiceName });
            } else if (wasRetCode == WASRETURNCODE_FAILED_CALLING_BPX4IPT) {
                // Native code failed to register new WLM Enclave into native registry
                Tr.audit(tc,
                         "NATIVE_SERVICE_BPX4IPT_FAILED",
                         new Object[] { wasServiceName });
            } else if (wasRetCode != 0) {
                // PC to authorized service failed
                Tr.audit(tc,
                         "NATIVE_SERVICE_PC",
                         new Object[] { serviceName, wasRetCode });
            } else {
                // WLM failed the service call
                Tr.audit(tc,
                         "NATIVE_SERVICE_WLM_FAILURE",
                         new Object[] { serviceName, reasonCode });
            }
        } else if (wasServiceName.equals(WLMServiceResults.AUTH_DisconnectAsWorkMgr)) {
            if (wasRetCode != 0) {
                // PC to authorized service failed
                Tr.audit(tc,
                         "NATIVE_SERVICE_PC",
                         new Object[] { serviceName, wasRetCode });
            } else {
                // WLM failed the service call
                Tr.audit(tc,
                         "NATIVE_SERVICE_WLM_FAILURE",
                         new Object[] { serviceName, reasonCode });
            }
        } else if (wasServiceName.equals(WLMServiceResults.AUTH_CreateWorkUnit) ||
                   wasServiceName.equals(WLMServiceResults.AUTH_CreateJoinWorkUnit)) {
            if (wasRetCode != 0) {
                if (WASRETURNCODE_FAILED_TO_REGISTER == wasRetCode) {
                    // Native code failed to register new WLM Enclave into native registry
                    Tr.audit(tc,
                             "NATIVE_SERVICE_FAILED_TO_REGISTER",
                             new Object[] { wasServiceName });
                } else if ((ERRNO.ENOMEM.errno() == wasRetCode)) {
                    // Native code run into NO MEMORY condition
                    Tr.audit(tc,
                             "NATIVE_SERVICE_OOM",
                             new Object[] { wasServiceName });
                } else {
                    // PC to authorized service failed
                    Tr.audit(tc,
                             "NATIVE_SERVICE_PC",
                             new Object[] { serviceName, wasRetCode });
                }
            } else {
                // WLM failed the service call
                Tr.audit(tc,
                         "NATIVE_SERVICE_WLM_FAILURE",
                         new Object[] { serviceName, reasonCode });
            }
        } else if (wasServiceName.equals(WLMServiceResults.AUTH_JoinWorkUnit) ||
                   wasServiceName.equals(WLMServiceResults.AUTH_LeaveWorkUnit) ||
                   wasServiceName.equals(WLMServiceResults.AUTH_DeleteWorkUnit) ||
                   wasServiceName.equals(WLMServiceResults.AUTH_LeaveDeleteWorkUnit)) {
            if (WASRETURNCODE_FAILED_TO_VALIDATE_IN_REGISTRY == wasRetCode) {
                // Native code failed to register new WLM Enclave into native registry
                Tr.audit(tc,
                         "NATIVE_SERVICE_REGISTRY_FAILED_VALIDATE",
                         new Object[] { wasServiceName });
            } else if (wasRetCode != 0) {
                // PC to authorized service failed
                Tr.audit(tc,
                         "NATIVE_SERVICE_PC",
                         new Object[] { serviceName, wasRetCode });
            } else {
                // WLM failed the service call
                Tr.audit(tc,
                         "NATIVE_SERVICE_WLM_FAILURE",
                         new Object[] { serviceName, reasonCode });
            }
        } else {
            // Non-specific service failed.
            Tr.audit(tc,
                     "NATIVE_SERVICE_FAILED_GENERIC",
                     new Object[] { wasServiceName, wasRetCode, serviceName, returnCode, reasonCode });
        }
    }

    /**
     * Get the <code>WLMServiceResults</code> associated with the current
     * thread.
     */
    public static WLMServiceResults getWLMServiceResult() {
        final WLMServiceResults result = results.get();

        return result;
    }

    /**
     * Get the WLM return code from the service that was performed.
     */
    public int getReturnCode() {
        return _returnCode;
    }

    /**
     * Get the WLM reason code from the service that was performed.
     */
    public int getReasonCode() {
        return _reasonCode;
    }

    /**
     * Get the name of the service that was last used.
     */
    public String getServiceName() {
        return _serviceName;
    }

    /**
     * Get the return code from the WebSphere service that processed the
     * request.
     */
    public int getWebSphereReturnCode() {
        return _wasReturnCode;
    }

    /**
     * Get the name of the WebSphere service that processed the request.
     */
    public String getWebSphereServiceName() {
        return _wasServiceName;
    }

    /**
     * Get the return data from the service call
     */
    public byte[] getReturnData() {
        return _returnData != null ? _returnData.clone() : null;
    }

    /**
     * Simple diagnostic aid.
     */
    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();

        sb.append("WebSphere service=").append(_wasServiceName);
        sb.append(", WebSphere returnCode=").append(_wasReturnCode);
        sb.append(", WLM service=").append(_serviceName);
        sb.append(", WLM product returnCode=").append(_returnCode);
        sb.append(", WLM product reasonCode=").append(_reasonCode);
        sb.append(", WLM return data=");
        if (_returnData != null)
            sb.append(bytesToString(_returnData));
        else
            sb.append("null");

        return sb.toString();
    }

    /**
     * Array used to convert a 4-bit binary value (nibble) to its equivalent
     * printable value.
     */
    private static final char[] charValues = { '0', '1', '2', '3', '4', '5',
                                               '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };

    /**
     * Make the printable version
     *
     * @param inBytes
     *                    byte[] to convert to String
     * @return converted bytes into String
     */
    @Trivial
    String bytesToString(byte[] inBytes) {
        String returnString = null;

        if (inBytes != null && inBytes.length > 0) {
            StringBuffer sb = new StringBuffer(inBytes.length * 2);
            // Loop thru each byte
            for (int i = 0; i < inBytes.length; i++) {
                // Isolate high and low order nibbles of current byte
                int hoNibble = (inBytes[i] & 0x000000F0) / 16;
                int loNibble = inBytes[i] & 0x0000000F;
                // Append char values of current nibbles
                sb.append(charValues[hoNibble]);
                sb.append(charValues[loNibble]);
            }
            returnString = sb.toString();
        }
        return returnString;
    }

    // -----------------------------------------------------------------
    // Return values for native services
    // -----------------------------------------------------------------

    // UnAuthorized WLM Services
    public static final String UNAUTH_ConnectAsWorkMgr = "ntv_le_connectAsWorkMgr";
    public static final String UNAUTH_CreateWorkUnit = "ntv_le_createWorkUnit";
    public static final String UNAUTH_DeleteWorkUnit = "ntv_le_deleteWorkUnit";
    public static final String UNAUTH_JoinWorkUnit = "ntv_le_joinWorkUnit";
    public static final String UNAUTH_LeaveWorkUnit = "ntv_le_leaveWorkUnit";
    public static final String UNAUTH_CreateJoinWorkUnit = "ntv_le_createJoinWorkUnit";
    public static final String UNAUTH_LeaveDeleteWorkUnit = "ntv_le_LeaveDeleteWorkUnit";

    // Authorized WLM Services
    public static final String AUTH_ConnectAsWorkMgr = "ntv_connectAsWorkMgr";
    public static final String AUTH_DisconnectAsWorkMgr = "ntv_disconnectAsWorkMgr";
    public static final String AUTH_CreateWorkUnit = "ntv_createWorkUnit";
    public static final String AUTH_DeleteWorkUnit = "ntv_deleteWorkUnit";
    public static final String AUTH_JoinWorkUnit = "ntv_joinWorkUnit";
    public static final String AUTH_LeaveWorkUnit = "ntv_leaveWorkUnit";
    public static final String AUTH_CreateJoinWorkUnit = "ntv_createJoinWorkUnit";
    public static final String AUTH_LeaveDeleteWorkUnit = "ntv_LeaveDeleteWorkUnit";
}
