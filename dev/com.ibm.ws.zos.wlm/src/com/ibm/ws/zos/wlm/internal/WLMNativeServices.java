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

import java.io.IOException;
import java.io.OutputStream;

/**
 * WLM Native Services
 */
public interface WLMNativeServices {
    public static final int WLM_MAXIMUM_TRANSACTIONCLASS_LENGTH = 8;
    public static final int WLM_DELETE_DATA_ENCLAVE_LENGTH = 8;
    public static final int WLM_DELETE_DATA_ENCLAVE_OFFSET = 56;

    /* Strings for map containing wlm data that MUST be kept in sync with: com.ibm.ws.zos.request.logging.internal.SmfDataRecorder */
    public static final int WLM_DELETE_DATA_LENGTH = 64;
    public static final String WLM_DATA_TRAN_CLASS = "wlmDataTranClass";
    public static final String WLM_DATA_HOST = "wlmDataHost";
    public static final String WLM_DATA_PORT = "wlmDataPort";
    public static final String WLM_DATA_URI = "wlmDataUri";
    public static final String WLM_DATA_ARRIVAL_TIME = "wlmDataArrivalTime";
    public static final String WLM_DATA_END_TIME = "wlmDataEndTime";
    public static final String WLM_DATA_DELETE_DATA = "wlmDataDeleteData";
    public static final String WLM_DATA_ENCLAVE_TOKEN = "wlmDataEnclaveToken";
    public static final String WLM_DATA_ENCLAVE_FORCED_DELETION = "wlmDataEnclaveForcedDeletion";

    boolean joinWorkUnit(byte[] token);

    boolean leaveWorkUnit(byte[] token);

    byte[] createWorkUnit(byte[] classificationInfo, long arrivalTime);

    byte[] createJoinWorkUnit(byte[] transactionClass, long arrivalTime);

    byte[] deleteWorkUnit(byte[] etoken);

    byte[] leaveDeleteWorkUnit(byte[] etoken);

    /**
     * Format and write diagnostics.
     *
     * @param out OutputStream to write diagnostics
     */
    void writeDiagnostics(OutputStream out) throws IOException;
}
