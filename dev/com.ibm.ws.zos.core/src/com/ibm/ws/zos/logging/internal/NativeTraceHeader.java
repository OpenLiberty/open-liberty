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

import com.ibm.websphere.ras.DataFormatHelper;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.zos.core.utils.internal.NativeUtilsImpl;

/**
 * This class builds the trace header for a native trace.
 */
@Trivial
public class NativeTraceHeader {

    static final int NATIVE_TRACE_PROBLEM_STATE = 1;
    static final int NATIVE_TRACE_SUPERVISOR_STATE = 0;
    static final int NATIVE_TRACE_KEY_8 = 8;
    static final int NATIVE_TRACE_KEY_2 = 2;

    private final int tracePoint;
    private final int tcbAddress;
    private final int key;
    private final int state;
    private final long createTime;

    /**
     * Construct a new instance of this class.
     *
     * @param tracePoint trace point identifier.
     * @param tcbAddress tcb address of thread that created the trace.
     * @param key        execution key at the time the trace was created.
     * @param state      execution state at the time the trace was created.
     * @param createTime stck containing the time the trace was created.
     */
    public NativeTraceHeader(int tracePoint, int tcbAddress, int key, int state, long createTime) {
        this.tracePoint = tracePoint;
        this.tcbAddress = tcbAddress;
        this.key = key;
        this.state = state;
        this.createTime = createTime;
    }

    /**
     * Create a trace header string containing information about the trace.
     *
     * @return a String containing the trace header information.
     */
    @Override
    public String toString() {
        long createTimeMillis = NativeUtilsImpl.getStckMillis(createTime);
        StringBuilder sb = new StringBuilder("Trace: ");
        sb.append(DataFormatHelper.formatTime(createTimeMillis));
        sb.append(" t=").append(Integer.toHexString(tcbAddress)).append(" key=");
        if (state == NATIVE_TRACE_PROBLEM_STATE) {
            sb.append("P");
        } else {
            sb.append("S");
        }
        sb.append(Integer.toHexString(key));
        sb.append(" (").append(Integer.toHexString(tracePoint)).append(")");
        return sb.toString();
    }

}
