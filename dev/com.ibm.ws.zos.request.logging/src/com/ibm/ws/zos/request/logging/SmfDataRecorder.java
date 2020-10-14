/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.zos.request.logging;

import java.util.Map;

/**
 * SMF recorder interface.
 */
public interface SmfDataRecorder {

    /** Generic SMF Record entry constants */
    public static final String END_STCK = "endStck";
    public static final String LOCAL_PORT = "localPort";
    public static final String MAPPED_USER_NAME = "mappedUserName";
    public static final String REMOTE_ADDR = "remoteAddr";
    public static final String REMOTE_PORT = "remotePort";
    public static final String REQUEST_ID = "requestId";
    public static final String REQUEST_URI = "requestUri";
    public static final String RESPONSE_BYTES = "responseBytes";
    public static final String START_STCK = "startStck";
    public static final String THREAD_ID = "threadId";
    public static final String THREAD_INFO = "threadInfo";
    public static final String TIMEUSED_START = "timeusedStart";
    public static final String TIMEUSED_END = "timeusedEnd";
    public static final String USER_NAME = "userName";
    public static final String API_USER_DATA = "apiUserData";

    /** WLM specific SMF Record entry constants. They MUST be kept in sync with com.ibm.ws.zos.wlm.WLMNativeServices */
    public static final String WLM_DATA_TRAN_CLASS = "wlmDataTranClass";
    public static final String WLM_DATA_HOST = "wlmDataHost";
    public static final String WLM_DATA_PORT = "wlmDataPort";
    public static final String WLM_DATA_URI = "wlmDataUri";
    public static final String WLM_DATA_ARRIVAL_TIME = "wlmDataArrivalTime";
    public static final String WLM_DATA_END_TIME = "wlmDataEndTime";
    public static final String WLM_DATA_DELETE_DATA = "wlmDataDeleteData";
    public static final String WLM_DATA_ENCLAVE_TOKEN = "wlmDataEnclaveToken";
    public static final String WLM_DATA_ENCLAVE_FORCED_DELETION = "wlmDataEnclaveForcedDeletion";
    public static final int WLM_DELETE_DATA_LENGTH = 64;

    /**
     * Build and write the SMF record.
     *
     * @param requestdata The data to be written to SMF.
     *
     * @return The SMF return code reflecting the status of the log request.
     */
    public int buildAndWriteRecord(Map<String, Object> requestdata);
}
