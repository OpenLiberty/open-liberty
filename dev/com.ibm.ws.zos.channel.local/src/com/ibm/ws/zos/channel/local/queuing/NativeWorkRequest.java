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
package com.ibm.ws.zos.channel.local.queuing;

import java.nio.ByteBuffer;
import java.util.Arrays;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.zos.channel.local.LocalCommClientConnHandle;
import com.ibm.ws.zos.channel.local.LocalCommDiscriminationData;

/**
 * Java implementation of a native LocalCommWorkRequestElement (BBGZLWQE).
 * These elements are queued to the black queue, selected by the BlackQueueListenerThread,
 * and dispatched to a registered callback handler via BlackQueueDemultiplexor.
 * 
 * These elements are also used for discrimination of upstream channels for a new connection.
 * 
 * Note: the NativeWorkRequest object associated with a new connection is
 * cached in LocalCommConnLink.
 * 
 * Note: this mapping must be kept in sync with the native mapping in
 * com.ibm.zos.native/include/server_local_comm_queue.h.
 */
public class NativeWorkRequest implements LocalCommDiscriminationData {

    /**
     * The maximum size use for DirectBufferHelper to use (size of Shared Memory
     * area containing Work requests (owned by BBGZLCOM)
     */
    public final static int MAX_NATIVE_SIZE = 4 * 1024 * 1024;

    private long nextWorkRequest;
    private NativeWorkRequestType requestType;
    private short requestFlags;
    private long createTime;
    private byte[] clientConnHandleBytes;
    private LocalCommClientConnHandle clientConnHandle;
    private byte[] requestSpecificParms;

    /**
     * NativeWorkRequest eyecatcher ('BBGZLWQE' in EBCDIC).
     */
    public static final long EyeCatcher = 0xc2c2c7e9d3e6d8c5L;

    /**
     * Offset to the eyecatcher field within the element.
     */
    public static final int EyeCatcherOffset = 0x10;

    /**
     * Offsets related to area returned from native code mapped by LocalCommWorkQueueElement
     * in server_local_comm_queue.h
     */
    protected static final int NEXT_WORKREQUEST_OFFSET = 0x008; /* stackElement_header.element_prev_p */
    protected static final int REQUEST_TYPE_OFFSET = 0x01C;
    protected static final int REQUESTFLAGS_OFFSET = 0x01E;
    protected static final int CREATE_STCK_OFFSET = 0x020;
    protected static final int LHDL_POINTER_OFFSET = 0x028;

    protected static final int LHDL_POINTER_LENGTH = 0x010; /* Client connection handle */
    
    protected static final int REQUESTSPECIFIC_PARMS_OFFSET = 0x038;
    protected static final int REQUESTSPECIFIC_PARMS_LENGTH = 0x048;

    /**
     * CONNECT Request Additional Parms
     * 
     * These offsets are relative to the requestSpecificParms section.
     */
    protected static final int CONNREQ_BBGZLDAT_P_OFFSET = 0x00;
    protected static final int CONNREQ_BBGZLOCL_P_OFFSET = 0x08;
    protected static final int CONNREQ_SHAREDMEM_USERTOKEN_OFFSET = 0x10;
    
    /**
     * REQUESTTYPE_FFDC additional parms.
     * 
     * These offsets are relative to the requestSpecificParms section.
     */
    protected static final int REQUESTTYPE_FFDC_TP_OFFSET = 0x00;
    protected static final int REQUESTTYPE_FFDC_RAWDATA_OFFSET = 0x04;
    public static final int REQUESTTYPE_FFDC_RAWDATA_SIZE = 0x44; // 68

    /**
     * Total size of native elements
     */
    protected static final int NATIVE_WORK_ELEMENT_SIZE = 0x80;

    /**
     * CTOR. Parses the data from the given native WRQE.
     * 
     * @param nativeWRQE
     */
    public NativeWorkRequest(ByteBuffer nativeWRQE) {

        // Map the variable argument list and set its initial position like va_start
        // does in native
        //ByteBuffer bbgzlhdl_bb = bufferHelper.getSlice(workRequest.getLHDLPointer(), MAX_NATIVE_SIZE);

        // TODO: we should at least verify the EYECATCHER here.  What, then, should
        //       happen if the data is bad?  Throw an exception?  We're on the BlackQueueListenerThread.
        //       Currently, if we throw an exception here, we WON'T process the rest of the
        //       requests in this chain.  However the BlackQueueListenerThread will survive
        //       and loop back around for the next set of requests.
        if (nativeWRQE != null) {
            nextWorkRequest = nativeWRQE.getLong(NEXT_WORKREQUEST_OFFSET);
            requestType = NativeWorkRequestType.forNativeValue(nativeWRQE.getShort(REQUEST_TYPE_OFFSET));
            requestFlags = nativeWRQE.getShort(REQUESTFLAGS_OFFSET);
            createTime = nativeWRQE.getLong(CREATE_STCK_OFFSET);
            nativeWRQE.mark(); // TODO: why do we need mark/reset?  
            clientConnHandleBytes = new byte[LHDL_POINTER_LENGTH];
            nativeWRQE.position(LHDL_POINTER_OFFSET);
            nativeWRQE.get(clientConnHandleBytes, 0, clientConnHandleBytes.length);
            requestSpecificParms = new byte[REQUESTSPECIFIC_PARMS_LENGTH];
            nativeWRQE.position(REQUESTSPECIFIC_PARMS_OFFSET);
            nativeWRQE.get(requestSpecificParms, 0, requestSpecificParms.length);
            nativeWRQE.reset();
        }
    }

    /**
     * Get the next "pointer"
     * 
     * @return the native address of the next WRQE
     */
    public long getNextWorkRequestPtr() {
        return nextWorkRequest;
    }

    /**
     * Get the Request type
     * 
     * @return the Request type
     */
    public NativeWorkRequestType getRequestType() {
        return requestType;
    }

    /**
     * Get the Request flags
     * 
     * @return the Request flags
     */
    short getRequestFlags() {
        return requestFlags;
    }

    /**
     * Get the time the request was created.
     * 
     * @return the time the request was created
     */
    long getCreateTime() {
        return createTime;
    }

    /**
     * @return the related shared memory token -- IF and ONLY IF this is a CONNECT work request;
     *         otherwise returns 0
     */
    public long getSharedMemoryToken() {
        return (getRequestType() == NativeWorkRequestType.REQUESTTYPE_CONNECT) ?
                        ByteBuffer.wrap(requestSpecificParms).getLong(NativeWorkRequest.CONNREQ_SHAREDMEM_USERTOKEN_OFFSET) :
                        0;
    }

    /**
     * Get the client connection handle
     */
    public LocalCommClientConnHandle getClientConnectionHandle() {
        if (clientConnHandle == null) {
            clientConnHandle = new LocalCommClientConnHandle(clientConnHandleBytes);
        }
        return clientConnHandle;
    }

    /**
     * Get the request specific parm area
     */
    public byte[] getRequestSpecificParms() {
        byte[] requestParmsCopy = null;
        if (requestSpecificParms != null) {
            requestParmsCopy = new byte[requestSpecificParms.length];
            System.arraycopy(requestSpecificParms, 0, requestParmsCopy, 0, requestSpecificParms.length);
        }

        return requestParmsCopy;
    }
    
    /**
     * @return the TP field from REQUESTTYPE_FFDC requests; or 0 if not a REQUESTTYPE_FFDC request.
     */
    public int getTP() {
        return (getRequestType() == NativeWorkRequestType.REQUESTTYPE_FFDC) ?
            ByteBuffer.wrap(requestSpecificParms).getInt(NativeWorkRequest.REQUESTTYPE_FFDC_TP_OFFSET) :
            0;
    }
    
    /**
     * @return the rawData field from REQUESTTYPE_FFDC requests; or null if not a REQUESTTYPE_FFDC request.
     */
    public byte[] getFFDCRawData() {
        return (getRequestType() == NativeWorkRequestType.REQUESTTYPE_FFDC) ?
                Arrays.copyOfRange(requestSpecificParms, NativeWorkRequest.REQUESTTYPE_FFDC_RAWDATA_OFFSET, requestSpecificParms.length) :
                null;
    }

    /**
     * Debugging and tracing aid.
     */
    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("\nNativeWorkRequest:" + super.toString());
        sb.append("\n\tnextWorkRequest:" + Long.toHexString(nextWorkRequest));
        sb.append("\n\trequestType:" + requestType);
        sb.append("\n\trequestFlags:" + Integer.toHexString(requestFlags));
        sb.append("\n\tcreateTime:" + Long.toHexString(createTime));
        sb.append("\n\tclientConnHandleBytes:" + toHexString(clientConnHandleBytes));

        String requestSpecificParmsStr = null;
        if (requestSpecificParms != null) {
            requestSpecificParmsStr = toHexString(requestSpecificParms);
        }
        sb.append("\n\trequestSpecificParms:" + requestSpecificParmsStr);

        return sb.toString();
    }

    final static String digits = "0123456789abcdef";

    /**
     * Converts a byte array to a hexadecimal string.
     */
    @Trivial
    public static String toHexString(byte[] b) {
        StringBuffer result = new StringBuffer(b.length * 2);
        for (int i = 0; i < b.length; i++) {
            result.append(digits.charAt((b[i] >> 4) & 0xf));
            result.append(digits.charAt(b[i] & 0xf));
        }
        return (result.toString());
    }

    /**
     * This method is invoked during the discrimination process by upstream
     * channels to determine if the upstream channel will accept the new connection
     * associated with this NativeWorkRequest.
     * 
     * @return WOLA_PROTOCOL (the only one we support at this time).
     */
    @Override
    public int getProtocol() {
        return LocalCommDiscriminationData.WOLA_PROTOCOL;
    }
}
