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
import java.nio.ByteBuffer;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.servlet.request.IRequest;
import com.ibm.ws.http.channel.outstream.HttpOutputStreamConnectWeb;
import com.ibm.ws.webcontainer31.osgi.osgi.WebContainerConstants;
import com.ibm.ws.webcontainer31.osgi.response.WCOutputStream31;

public class WCOutputStream61 extends WCOutputStream31 {
    private static final TraceComponent tc = Tr.register(WCOutputStream61.class, WebContainerConstants.TR_GROUP, WebContainerConstants.NLS_PROPS);

    public WCOutputStream61(HttpOutputStreamConnectWeb stream, IRequest req) {
        super(stream, req);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "constructor, out-->" + (output != null ? output : "null"));
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see jakarta.servlet.ServletOutStream#write(java.nio.ByteBuffer)
     */
    @Override
    public void write(ByteBuffer buffer) throws IOException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.entry(tc, "write(ByteBuffer)", "this [" + this + "]");
        }

        if (buffer == null) {
            Tr.error(tc, nls.getString("read.write.bytebuffer.null"));
            throw new NullPointerException(nls.getString("read.write.bytebuffer.null"));
        }

        if (!isReady()) {
            Tr.error(tc, nls.getString("read.write.failed.isReady.false"));
            throw new IllegalStateException(nls.getString("read.write.failed.isReady.false"));
        }

        if (buffer.remaining() == 0) {
            return;
        }

        byte[] b = new byte[buffer.remaining()];
        buffer.get(b);

        super.write(b);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.exit(tc, "write(ByteBuffer)");
        }
    }
}
