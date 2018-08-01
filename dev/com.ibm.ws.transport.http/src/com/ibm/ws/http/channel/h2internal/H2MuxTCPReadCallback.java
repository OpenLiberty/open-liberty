/*******************************************************************************
 * Copyright (c) 1997, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.http.channel.h2internal;

import java.io.IOException;
import java.net.SocketTimeoutException;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.http.channel.internal.HttpMessages;
import com.ibm.wsspi.bytebuffer.WsByteBuffer;
import com.ibm.wsspi.channelfw.VirtualConnection;
import com.ibm.wsspi.tcpchannel.TCPReadCompletedCallback;
import com.ibm.wsspi.tcpchannel.TCPReadRequestContext;

public class H2MuxTCPReadCallback implements TCPReadCompletedCallback {

    H2InboundLink connLink = null;

    /** RAS tracing variable */
    private static final TraceComponent tc = Tr.register(H2MuxTCPReadCallback.class, HttpMessages.HTTP_TRACE_NAME, HttpMessages.HTTP_BUNDLE);

    public void setConnLinkCallback(H2InboundLink _link) {
        connLink = _link;
    }

    @Override
    public void complete(VirtualConnection vc, TCPReadRequestContext rrc) {

        if (connLink != null) {
            connLink.setReadLinkStatusToNotReadingAndNotify();
            connLink.processRead(vc, rrc);
        }
    }

    @Override
    public void error(VirtualConnection vc, TCPReadRequestContext rrc, IOException exception) {

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "H2MuxTCPReadCallback error callback called with exception: " + exception);
        }

        if (connLink != null) {

            // if there was a timeout, and write activity occurred, then go back to reading if we are not closing already
            if (exception instanceof SocketTimeoutException) {

                int rTimeout = connLink.getconfiguredInactivityTimeout();
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "H2MuxTCPReadCallback error: configured readTimeout: " + rTimeout);
                }

                if ((rTimeout != 0) && connLink.checkIfGoAwaySendingOrClosing() == false) {
                    long wLastTime = connLink.getLastWriteTime();
                    long diff = System.nanoTime() - wLastTime;

                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "H2MuxTCPReadCallback error: last write time: " + wLastTime + " diff: " + diff);
                    }

                    if (diff < rTimeout) {
                        // write occurred while the read got a timeout, so don't close the connection
                        int nextTimeout = rTimeout - (int) diff;
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "H2MuxTCPReadCallback error: continuing reading, setting nextTimeout to: " + nextTimeout);
                        }

                        connLink.setReadLinkStatusToNotReadingAndNotify();
                        connLink.processRead(vc, rrc, nextTimeout);
                        return;
                    }
                }
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "H2MuxTCPReadCallback error: closing connection");
            }

            connLink.setReadLinkStatusToNotReadingAndNotify();

            // release the read buffer since read failed
            if (connLink.getFreeBufferOnError()) {
                WsByteBuffer wsbb = rrc.getBuffer();
                if (wsbb != null) {
                    wsbb.release();
                    rrc.setBuffer(null);
                }
            }

            if (exception instanceof SocketTimeoutException) {
                //should send goaway on timeout exception
                connLink.closeConnectionLink(exception, true);
            } else {
                connLink.closeConnectionLink(exception, false);
            }
        }
    }

}
