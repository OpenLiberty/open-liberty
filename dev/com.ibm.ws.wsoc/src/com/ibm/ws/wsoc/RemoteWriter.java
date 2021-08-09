/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.wsoc;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.wsoc.MessageWriter.WRITE_TYPE;
import com.ibm.ws.wsoc.WsocConnLink.RETURN_STATUS;
import com.ibm.wsspi.bytebuffer.WsByteBuffer;
import com.ibm.wsspi.bytebuffer.WsByteBufferPoolManager;
import com.ibm.wsspi.tcpchannel.TCPRequestContext;

public class RemoteWriter extends OutputStreamWriter {

    private static final TraceComponent tc = Tr.register(RemoteWriter.class);

    ByteArrayOutputStream underlyingsStream = null;
    WsocConnLink connLink = null;

    public RemoteWriter(ByteArrayOutputStream os) {

        super(os);
        underlyingsStream = os;
    }

    public void initialize(WsocConnLink link) {
        connLink = link;
    }

    @Override
    @FFDCIgnore(IOException.class)
    public void close() {

        // from RemoteEndpoint.Basic javadoc:
        // The developer must close the writer in order to indicate that the complete message has been placed into the character stream.
        byte[] ba = null;

        try {
            super.flush();
            ba = underlyingsStream.toByteArray();
            super.close();
        } catch (IOException x) {
            // do NOT allow instrumented FFDC to be used here
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "IOException caught while closing: " + x);
            }
        }

        // Performance: wait until close to write out the message,  any way we could make this better for large message writes?
        WsByteBufferPoolManager bufferManager = connLink.getBufferManager();
        WsByteBuffer buffer = bufferManager.wrap(ba);
        RETURN_STATUS ret = connLink.writeBuffer(buffer, OpcodeType.TEXT_WHOLE, WRITE_TYPE.SYNC, null, TCPRequestContext.NO_TIMEOUT, false, false);

        if (ret != RETURN_STATUS.OK) {
            IOException ioe = new IOException("Data could not be written using WebSocket Writer object");
            connLink.callOnError(ioe);
        }
    }

}
