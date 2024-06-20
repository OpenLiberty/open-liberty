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
package com.ibm.ws.zos.logging.internal;

import java.nio.ByteBuffer;

import com.ibm.websphere.ras.annotation.Trivial;

/**
 * Trace data returned by the native code.
 */
@Trivial
public class NativeTraceData {

    private int traceLevel;
    private int tracePoint;
    private long varargListPointer;
    private long traceCreateTime;
    private int traceCreateTcb;
    private int traceCreateState;
    private int traceCreateKey;

    /*
     * Offsets into byte[] returned from native code mapped by OutputParms
     * in server_tracing_functions.c
     */
    protected static final int TRACE_LEVEL_OFFSET = 0x00;
    protected static final int TRACE_POINT_OFFSET = 0x04;
    protected static final int VA_LIST_OFFSET = 0x08;
    protected static final int TRACE_CREATE_TIME_OFFSET = 0x10;
    protected static final int TRACE_CREATE_TCB_OFFSET = 0x18;
    protected static final int TRACE_CREATE_STATE_OFFSET = 0x1c;
    protected static final int TRACE_CREATE_KEY_OFFSET = 0x20;

    public NativeTraceData(final byte[] nativeNTD) {

        if (nativeNTD != null && nativeNTD.length != 0) {
            byte[] traceData = nativeNTD;
            ByteBuffer buf = ByteBuffer.wrap(traceData);
            traceLevel = buf.getInt(TRACE_LEVEL_OFFSET);
            tracePoint = buf.getInt(TRACE_POINT_OFFSET);
            varargListPointer = buf.getLong(VA_LIST_OFFSET);
            traceCreateTime = buf.getLong(TRACE_CREATE_TIME_OFFSET);
            traceCreateTcb = buf.getInt(TRACE_CREATE_TCB_OFFSET);
            traceCreateState = buf.getInt(TRACE_CREATE_STATE_OFFSET);
            traceCreateKey = buf.getInt(TRACE_CREATE_KEY_OFFSET);
        }
    }

    /**
     * Get the trace level.
     *
     * @return the trace level
     */
    int getTraceLevel() {
        return traceLevel;
    }

    /**
     * Get the trace point.
     *
     * @return the trace point
     */
    int getTracePoint() {
        return tracePoint;
    }

    /**
     * Get the valist.
     *
     * @return the valist
     */
    long getVarargListPointer() {
        return varargListPointer;
    }

    /**
     * Get the time the trace was created.
     *
     * @return the time the trace was created
     */
    long getCreateTime() {
        return traceCreateTime;
    }

    /**
     * Get the TCB address of the thread the created the trace.
     *
     * @return the creating tcb address
     */
    int getCreatingTcb() {
        return traceCreateTcb;
    }

    /**
     * Get the state of the thread the created the trace.
     *
     * @return 1 problem state 0 supervisor state
     */
    int getCreateState() {
        return traceCreateState;
    }

    /**
     * Get the key of the thread the created the trace.
     *
     * @return key of the thread the created the trace
     */
    int getCreateKey() {
        return traceCreateKey;
    }
}
