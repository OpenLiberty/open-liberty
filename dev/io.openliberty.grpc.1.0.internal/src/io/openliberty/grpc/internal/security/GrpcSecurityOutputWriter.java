package io.openliberty.grpc.internal.security;

/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

import java.io.OutputStream;
import java.io.PrintWriter;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import io.openliberty.grpc.internal.GrpcMessages;

/**
 * 
 * To override the security committed response. create a writer with
 * outputStream.
 *
 */
public class GrpcSecurityOutputWriter extends PrintWriter {

    private static final TraceComponent tc = Tr.register(GrpcSecurityOutputWriter.class, GrpcMessages.GRPC_TRACE_NAME,
            GrpcMessages.GRPC_BUNDLE);

    public GrpcSecurityOutputWriter(OutputStream out) {
        super(out);

    }

    @Override
    public void flush() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "override security response, dont flush to stream");
        }
    }

    @Override
    public void write(int c) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "override security response, dont write to stream");
        }
    }

    @Override
    public void write(char buf[], int off, int len) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "override security response, dont write to stream");
        }
    }

    @Override
    public void write(char buf[]) {
        write(buf, 0, buf.length);
    }

    @Override
    public void write(String s, int off, int len) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "override security response, dont write to stream");
        }
    }

}
