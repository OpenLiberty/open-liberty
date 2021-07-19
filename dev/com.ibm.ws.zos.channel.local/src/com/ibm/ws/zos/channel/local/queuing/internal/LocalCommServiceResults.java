/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.zos.channel.local.queuing.internal;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.websphere.ras.annotation.Trivial;

/**
 *
 */
public class LocalCommServiceResults {
    /**
     * TraceComponent for this class.
     */
    private final static TraceComponent tc = Tr.register(LocalCommServiceResults.class);

    /**
     * Thread level data to hold results of Local Comm calls.
     */
    private final static class LocalCommResultsThreadLocal extends ThreadLocal<LocalCommServiceResults> {
        @Override
        @Sensitive
        protected LocalCommServiceResults initialValue() {
            return new LocalCommServiceResults();
        }

        @Override
        public String toString() {
            return get().toString();
        }
    }

    /**
     * This object holds the results of the last Local Comm call that was made in
     * native code. It's intended to be similar to the concept of errno and
     * errno2 in Unix and C libraries. <br>
     * If you get a bad return code from one of the services, you can look at
     * this object for more information on the service that was called and its
     * return codes.
     */
    private final static ThreadLocal<LocalCommServiceResults> results = new LocalCommResultsThreadLocal();

    /**
     * Local Comm return code.
     */
    private int _returnCode;

    /**
     * Local Comm reason code.
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
     * part server_lcom_services.h
     */
    // zzzz

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
    protected LocalCommServiceResults() {}

    /**
     * Set the Local Comm codes and service name.
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

        LocalCommServiceResults result = getLComServiceResult();

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
    public void issueLComServiceMessage() {

        int wasRetCode = _wasReturnCode;
        int returnCode = _returnCode;
        int reasonCode = _reasonCode;

        if ((_serviceName == null) && (_wasServiceName == null))
            return;

        String serviceName = String.valueOf(_serviceName);
        String wasServiceName = String.valueOf(_wasServiceName);

        if (wasServiceName.equals(LocalCommServiceResults.NTV_SERV_initializeChannel)) {
            if ((returnCode != 0) && (reasonCode != 0)) {
                // Local Comm failed the service call
                Tr.audit(tc,
                         "NATIVE_SERVICE_LCOM_FAILURE",
                         new Object[] { serviceName, reasonCode });
            } else if (wasRetCode != 0) {
                // PC to authorized service failed
                Tr.audit(tc,
                         "NATIVE_SERVICE_PC",
                         new Object[] { serviceName, wasRetCode });
            }
        } else if (wasServiceName.equals(LocalCommServiceResults.NTV_SERV_uninitializeChannel) || 
        		   wasServiceName.equals(LocalCommServiceResults.NTV_SERV_connectResponse) ||
        		   wasServiceName.equals(LocalCommServiceResults.NTV_SERV_connectToClientsSharedMemory) ||
        		   wasServiceName.equals(LocalCommServiceResults.NTV_SERV_stopListeningForRequests) ||
        		   wasServiceName.equals(LocalCommServiceResults.NTV_SERV_freeWorkRequestElements) ||
        		   wasServiceName.equals(LocalCommServiceResults.NTV_SERV_getWorkRequestElements)
        		   ) {
            if ((returnCode == 0) && (reasonCode == 0)) {
                // PC to authorized service failed
                Tr.audit(tc,
                         "NATIVE_SERVICE_PC",
                         new Object[] { serviceName, wasRetCode });
            } else {
                // Non-specific service failed.
                Tr.audit(tc,
                         "NATIVE_SERVICE_FAILED_GENERIC",
                         new Object[] { wasServiceName, wasRetCode, serviceName, returnCode, reasonCode });
            }
        } else {
            // Non-specific service failed.
            Tr.audit(tc,
                     "NATIVE_SERVICE_FAILED_GENERIC",
                     new Object[] { wasServiceName, wasRetCode, serviceName, returnCode, reasonCode });
        }
    }

    /**
     * Get the <code>LocalCommServiceResults</code> associated with the current
     * thread.
     */
    public static LocalCommServiceResults getLComServiceResult() {
        final LocalCommServiceResults result = results.get();

        return result;
    }

    /**
     * Get the Local Comm return code from the service that was performed.
     */
    public int getReturnCode() {
        return _returnCode;
    }

    /**
     * Get the Local Comm reason code from the service that was performed.
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
        sb.append(", Local Comm service=").append(_serviceName);
        sb.append(", Local Comm product returnCode=").append(_returnCode);
        sb.append(", Local Comm product reasonCode=").append(_reasonCode);
        sb.append(", Local Comm return data=");
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
     *            byte[] to convert to String
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

    public static final String NTV_SERV_initializeChannel = "ntv_initializeChannel";
    public static final String NTV_SERV_uninitializeChannel = "ntv_uninitializeChannel"; 
    public static final String NTV_SERV_getWorkRequestElements = "ntv_getWorkRequestElements";
    public static final String NTV_SERV_freeWorkRequestElements = "ntv_freeWorkRequestElements";
    public static final String NTV_SERV_stopListeningForRequests = "ntv_stopListeningForRequests";
    public static final String NTV_SERV_connectResponse = "ntv_connectResponse";
    public static final String NTV_SERV_connectToClientsSharedMemory = "ntv_connectToClientsSharedMemory";
    
}