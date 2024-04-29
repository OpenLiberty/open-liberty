/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.webcontainer61.osgi.response;

import java.io.IOException;

import jakarta.servlet.ServletOutputStream;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.servlet.request.IRequest;
import com.ibm.ws.http.channel.outstream.HttpOutputStreamConnectWeb;
import com.ibm.ws.webcontainer.osgi.osgi.WebContainerConstants;
import com.ibm.wsspi.http.HttpInboundConnection;

import io.openliberty.webcontainer60.osgi.response.IResponse60Impl;

/*
 * Support ServletOutputStream write(ByteBuffer)
 */

public class IResponse61Impl extends IResponse60Impl {
    private static final TraceComponent tc = Tr.register(IResponse61Impl.class, WebContainerConstants.TR_GROUP, WebContainerConstants.NLS_PROPS);

    public IResponse61Impl(IRequest req, HttpInboundConnection connection) {
        super(req, connection);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "constructor , req [" + req + "] , inboundConnection [" + connection + "]");
        }
    }

    /*
     * Return the OutputStream61 which implements the write(ByteBuffer)
     */
    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "getOutputStream , this [" + this + "]");
        }

        if (null == this.outStream) {
            this.outStream = new WCOutputStream61((HttpOutputStreamConnectWeb) this.response.getBody(), this.request);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "getOutputStream , return outStream [" + this.outStream + "]");
        }
        return this.outStream;
    }
}
