/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.tcpchannel.internal;

import java.io.IOException;
import java.net.SocketTimeoutException;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.wsspi.bytebuffer.WsByteBuffer;
import com.ibm.wsspi.channelfw.ConnectionReadyCallback;
import com.ibm.wsspi.channelfw.DiscriminationProcess;
import com.ibm.wsspi.channelfw.VirtualConnection;
import com.ibm.wsspi.channelfw.exception.DiscriminationProcessException;
import com.ibm.wsspi.tcpchannel.TCPReadCompletedCallback;
import com.ibm.wsspi.tcpchannel.TCPReadRequestContext;
import com.ibm.wsspi.tcpchannel.TCPRequestContext;

/**
 * Implements the callback for the initial read on a new connection
 * and calls the discrimination process in order to complete the vc.
 * This class is used by the TCPPort class
 */
public class NewConnectionInitialReadCallback implements TCPReadCompletedCallback {

    private final TCPChannel tcpChannel;

    private static final TraceComponent tc = Tr.register(NewConnectionInitialReadCallback.class, TCPChannelMessageConstants.TCP_TRACE_NAME, TCPChannelMessageConstants.TCP_BUNDLE);

    /**
     * Constructor.
     *
     * @param _tcpChannel
     */
    public NewConnectionInitialReadCallback(TCPChannel _tcpChannel) {
        this.tcpChannel = _tcpChannel;
    }

    /**
     * Tests if the request has space in its buffer(s) or not.
     *
     * @param req
     * @return boolean
     */
    private boolean requestFull(TCPReadRequestContext req) {
        WsByteBuffer wsBuffArray[] = req.getBuffers();
        boolean rc = !wsBuffArray[wsBuffArray.length - 1].hasRemaining();

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "requestFull: " + rc);
        }
        return rc;
    }

    /*
     * @see
     * com.ibm.wsspi.tcpchannel.TCPReadCompletedCallback#complete(com.ibm.wsspi
     * .channelfw.VirtualConnection,
     * com.ibm.wsspi.tcpchannel.TCPReadRequestContext)
     */
    @Override
    public void complete(VirtualConnection vc, TCPReadRequestContext req) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "complete");
        }

        sendToDiscriminators(vc, req, false);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "complete");
        }
    }

    /*
     * @see
     * com.ibm.wsspi.tcpchannel.TCPReadCompletedCallback#error(com.ibm.wsspi.channelfw
     * .VirtualConnection, com.ibm.wsspi.tcpchannel.TCPReadRequestContext,
     * java.io.IOException)
     */
    @Override
    public void error(VirtualConnection vc, TCPReadRequestContext req, IOException ioe) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "error: " + vc);
        }
        // This can legitemately happen if the client closes
        // the connection before sending data - we will always get
        // an exception in that case
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "Error occurred during initial read of request from client: " + req.getInterface().getRemoteAddress() + " " + req.getInterface().getRemotePort() + " : "
                         + ioe);
        }

        // release any WsByteBuffers that were allocated for this request
        WsByteBuffer buffArray[] = req.getBuffers();
        if (buffArray != null) {
            for (int i = 0; i < buffArray.length; i++) {
                if (buffArray[i] != null) {
                    buffArray[i].release();
                }
            }
        }
        req.setBuffers(null);

        // if read timed out, send to discriminators so that they
        // can send error response if necessary
        if (ioe != null && ioe instanceof SocketTimeoutException) {
            sendToDiscriminators(vc, req, true);
        } else {
            // Destroy the connection link
            ((TCPReadRequestContextImpl) req).getTCPConnLink().close(vc, ioe);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "error");
        }
    }

    /**
     * invoke discrimination process.
     *
     * @param inVC
     * @param req
     * @param errorOnRead
     */
    private void sendToDiscriminators(VirtualConnection inVC, TCPReadRequestContext req, boolean errorOnRead) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "sendToDiscriminators");
        }
        boolean doAgain;
        req.setJITAllocateSize(0); // JIT Allocate was on for the initial read,
                                   // reset it
        TCPConnLink conn = ((TCPReadRequestContextImpl) req).getTCPConnLink();
        VirtualConnection vc = inVC;
        do {
            doAgain = false;
            int state;
            try {
                state = tcpChannel.getDiscriminationProcess().discriminate(vc, req.getBuffers(), conn);
            } catch (DiscriminationProcessException dpe) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                    Tr.event(tc, "Exception occurred while discriminating data received from client " + req.getInterface().getRemoteAddress() + " "
                                 + req.getInterface().getRemotePort());
                ((TCPReadRequestContextImpl) req).getTCPConnLink().close(vc, new IOException("Discrimination failed " + dpe.getMessage()));
                break;
            }
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Discrimination returned " + state);
            }

            if (state == DiscriminationProcess.SUCCESS) {

                ConnectionReadyCallback cb = conn.getApplicationCallback();
                // is cb is null, then connlink may have been destroyed by channel stop
                // if so, nothing more needs to be done
                if (cb != null) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                        Tr.event(tc, "Calling application callback.ready method");
                    }
                    cb.ready(vc);
                } else {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                        Tr.event(tc, "No application callback found, closing connection");
                    }
                    ((TCPReadRequestContextImpl) req).getTCPConnLink().close(vc, null);
                }
            } else if (state == DiscriminationProcess.AGAIN) {
                if (errorOnRead) { // error on first read, don't retry
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                        Tr.event(tc, "First read timed out, and more than one discriminator asked for more data" + req.getInterface().getRemoteAddress() + " "
                                     + req.getInterface().getRemotePort());
                    ((TCPReadRequestContextImpl) req).getTCPConnLink().close(vc, null);
                } else if (requestFull(req)) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                        Tr.event(tc, "Discrimination failed, no one claimed data even after 1 complete buffer presented - probably garbage passed in"
                                     + req.getInterface().getRemoteAddress()
                                     + " " + req.getInterface().getRemotePort());
                    ((TCPReadRequestContextImpl) req).getTCPConnLink().close(vc, null);

                } else {
                    vc = req.read(1, this, false, TCPRequestContext.USE_CHANNEL_TIMEOUT);
                    if (vc != null) {
                        doAgain = true;
                    }

                }
            } else { // FAILURE
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                    Tr.event(tc, "Error occurred while discriminating data received from client " + req.getInterface().getRemoteAddress() + " "
                                 + req.getInterface().getRemotePort());
                ((TCPReadRequestContextImpl) req).getTCPConnLink().close(vc, null);
            }
        } while (doAgain);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "sendToDiscriminators");
        }
    }

}
