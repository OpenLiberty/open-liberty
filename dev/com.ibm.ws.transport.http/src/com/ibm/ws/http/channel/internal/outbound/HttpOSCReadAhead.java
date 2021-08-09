/*******************************************************************************
 * Copyright (c) 2004, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.http.channel.internal.outbound;

import java.io.IOException;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.http.channel.internal.CallbackIDs;
import com.ibm.ws.http.channel.internal.HttpMessages;
import com.ibm.wsspi.channelfw.VirtualConnection;
import com.ibm.wsspi.tcpchannel.TCPReadCompletedCallback;
import com.ibm.wsspi.tcpchannel.TCPReadRequestContext;

/**
 * This represents the asynchronous callback used for the outbound read-ahead
 * ability when connections are placed in a connection pool. It's primary use
 * is for the error() callback to detect when the server side closes a socket
 * and this callback will then notify the application channel that the outbound
 * connection is no longer open.
 */
public class HttpOSCReadAhead implements TCPReadCompletedCallback {

    /** RAS trace variable */
    private static final TraceComponent tc = Tr.register(HttpOSCReadAhead.class, HttpMessages.HTTP_TRACE_NAME, HttpMessages.HTTP_BUNDLE);

    /** Singleton instance of this class */
    private static final HttpOSCReadAhead myInstance = new HttpOSCReadAhead();

    /**
     * Private constructor, use the getRef() method for access.
     */
    private HttpOSCReadAhead() {
        // nothing to do
    }

    /**
     * Get access to the singleton instance of this class.
     * 
     * @return HttpOSCReadAhead
     */
    public static final HttpOSCReadAhead getRef() {
        return myInstance;
    }

    /**
     * If the read completes with data, then this method will be called. It
     * then depends on what state the actual connection is in on what should
     * happen next.
     * 
     * @param vc
     * @param rsc
     */
    public void complete(VirtualConnection vc, TCPReadRequestContext rsc) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "complete() called: vc=" + vc);
        }

        HttpOutboundServiceContextImpl osc = (HttpOutboundServiceContextImpl) vc.getStateMap().get(CallbackIDs.CALLBACK_HTTPOSC);
        osc.markReadCancelFailure();

        int state;
        // query/set the states in one block to avoid timing windows
        synchronized (osc.stateSyncObject) {
            state = osc.getReadState();
            osc.setCallbackState(HttpOutboundServiceContextImpl.CALLBACK_STATE_COMPLETE, null);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Read-ahead state: " + state);
        }

        switch (state) {
            case (HttpOutboundServiceContextImpl.READ_STATE_IDLE):
                // connection is still in app channel's pool, error condition
                IOException ioe = new IOException("Unexpected read complete");
                osc.getAppReadCallback().error(vc, ioe);
                break;
            case (HttpOutboundServiceContextImpl.READ_STATE_TIME_RESET):
                // init() has been called and the read-ahead should have been
                // canceled, so this is an error
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Error: Read-ahead completed after init() call.");
                }
                osc.setCallbackState(HttpOutboundServiceContextImpl.CALLBACK_STATE_ERROR, new IOException("Invalid read-ahead data"));
                break;
            case (HttpOutboundServiceContextImpl.READ_STATE_SYNC):
                // d264854: no longer possible
                // sync read for the response has already started
                osc.wakeupReadAhead();
                break;
            case (HttpOutboundServiceContextImpl.READ_STATE_ASYNC):
                // d264854: no longer possible
                // async read for the response has already started
                HttpOSCReadCallback.getRef().complete(vc, rsc);
                break;
            default:
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Unexpected read-ahead state: " + state);
                }
                break;
        }
    }

    /**
     * If an error occurs, such as the server side closing down the socket, then
     * this method will be called. Depending on what state the connection is in,
     * this error is either sent to the application channel immediately or
     * delayed until the actual read for the response would start, when it can
     * then hand the error off to the application channel.
     * 
     * @param vc
     * @param rsc
     * @param ioe
     */
    public void error(VirtualConnection vc, TCPReadRequestContext rsc, IOException ioe) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "error() called: vc=" + vc + " ioe=" + ioe);
        }

        HttpOutboundServiceContextImpl osc = (HttpOutboundServiceContextImpl) vc.getStateMap().get(CallbackIDs.CALLBACK_HTTPOSC);
        if (osc.markReadCancelSuccess()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Ignoring error callback on canceled read");
            }
            return;
        }

        int state;
        // query/set the states in one block to avoid timing windows
        synchronized (osc.stateSyncObject) {
            state = osc.getReadState();
            osc.setCallbackState(HttpOutboundServiceContextImpl.CALLBACK_STATE_ERROR, ioe);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Read-ahead state: " + state);
        }
        switch (state) {
            case (HttpOutboundServiceContextImpl.READ_STATE_IDLE):
                // new connection hasn't started yet, notify app channel now
                osc.getAppReadCallback().error(vc, ioe);
                break;
            case (HttpOutboundServiceContextImpl.READ_STATE_TIME_RESET):
                // new conn has been initialized but the read for response hasn't
                // been started yet
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Received the read-ahead immed timeout.");
                }
                break;
            case (HttpOutboundServiceContextImpl.READ_STATE_SYNC):
                // d264854: no longer possible
                // a synchronous read for the response has been started already
                osc.wakeupReadAhead();
                break;
            case (HttpOutboundServiceContextImpl.READ_STATE_ASYNC):
                // d264854: no longer possible
                // an async read for the response has been started already
                osc.setPersistent(false);
                osc.reConnect(vc, ioe);
                break;
            default:
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Unexpected read-ahead state: " + state);
                }
                break;
        }
    }

}