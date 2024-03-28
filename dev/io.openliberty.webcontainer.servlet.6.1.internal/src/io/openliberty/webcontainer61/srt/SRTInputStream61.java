/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.webcontainer61.srt;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.logging.Level;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.webcontainer31.osgi.osgi.WebContainerConstants;
import com.ibm.ws.webcontainer31.srt.SRTInputStream31;

public class SRTInputStream61 extends SRTInputStream31 {
    private static final String CLASS_NAME = SRTInputStream61.class.getName();
    private static final TraceComponent tc = Tr.register(SRTInputStream61.class, WebContainerConstants.TR_GROUP, WebContainerConstants.NLS_PROPS);

    public SRTInputStream61(SRTServletRequest61 request) {
        super(request);

        if (TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
            Tr.debug(tc, "constructor, SRTInputStream61 [" + this + "] , request [" + request + "]");
        }
    }
    
    @Override
    public void init(InputStream in) throws IOException {
        if (TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
            Tr.debug(tc, "Initializing ... passing through ; underlying stream [" + in +"] ; this [" + this + "]");
        }
        super.init(in);
    }

    /*
     * (non-Javadoc)
     *
     * @see jakarta.servlet.ServletInputStream#read(java.nio.ByteBuffer)
     */
    @Override
    public int read(ByteBuffer buffer) throws IOException {
        if (TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
            Tr.entry(tc, "read(ByteBuffer)", " this [" + this + "]");
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
            return 0;
        }

        byte[] b = new byte[buffer.remaining()];

        int returnByte = read(b);
        if (returnByte == -1) {
            return -1;
        }

        int position = buffer.position();

        buffer.put(b, 0, returnByte);
        buffer.position(position);
        buffer.limit(position + returnByte);

        if (TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
            Tr.exit(tc, "read(ByteBuffer) : [" + returnByte + "]");
        } 
        return returnByte;
    }
}
