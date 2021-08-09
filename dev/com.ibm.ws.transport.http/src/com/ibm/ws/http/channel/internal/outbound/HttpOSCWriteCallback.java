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
import com.ibm.wsspi.tcpchannel.TCPWriteCompletedCallback;
import com.ibm.wsspi.tcpchannel.TCPWriteRequestContext;

/**
 * Callback class used while writing any part of the outgoing request message,
 * such as the headers or the body.
 */
public class HttpOSCWriteCallback implements TCPWriteCompletedCallback {

    /** RAS tracing variable */
    private static final TraceComponent tc = Tr.register(HttpOSCWriteCallback.class, HttpMessages.HTTP_TRACE_NAME, HttpMessages.HTTP_BUNDLE);

    /** Singleton instance of this class */
    private static final HttpOSCWriteCallback myInstance = new HttpOSCWriteCallback();

    /**
     * Private constrctor, use the getRef() API for access.
     */
    private HttpOSCWriteCallback() {
        // nothing to do
    }

    /**
     * Get access to the singleton instance of this callback.
     * 
     * @return HttpOSCWriteCallback
     */
    public static final HttpOSCWriteCallback getRef() {
        return myInstance;
    }

    /**
     * Called by the TCP channel when the write has finished.
     * 
     * @param vc
     * @param wsc
     */
    public void complete(VirtualConnection vc, TCPWriteRequestContext wsc) {

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "complete() called: vc=" + vc);
        }
        HttpOutboundServiceContextImpl mySC = (HttpOutboundServiceContextImpl) vc.getStateMap().get(CallbackIDs.CALLBACK_HTTPOSC);
        // LI4335 - handle early reads
        if (mySC.isEarlyRead()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Notifying app channel of write complete");
            }
            mySC.getAppWriteCallback().complete(vc);
            return;
        }
        // if only the headers have been sent, then we need to check some
        // special case handling scenarios
        // 381105 - only start response read after just the headers have been
        // sent and no body... eventually need to get the isPartialBody state
        // implemented to make this simpler.
        if (mySC.isHeadersSentState() && 0 == mySC.getNumBytesWritten()) {
            if (mySC.shouldReadResponseImmediately()) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Sent headers, reading for response");
                }
                mySC.startResponseRead();
                return;
            }
        }

        // if we're here and we need to, notify the channel above that the
        // write has completed, otherwise start the read for the response
        // message
        if (!mySC.isMessageSent()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Calling write complete callback of app channel.");
            }
            mySC.getAppWriteCallback().complete(vc);
        } else {
            if (mySC.shouldReadResponseImmediately()) {
                // we've already done the "read first" call so jump to the
                // regular method now
                mySC.readAsyncResponse();
            } else {
                // initial read for a response
                mySC.startResponseRead();
            }
        }
    }

    /**
     * An error occurred while writing part or all of a request out.
     * 
     * @param vc
     * @param wsc
     * @param ioe
     */
    public void error(VirtualConnection vc, TCPWriteRequestContext wsc, IOException ioe) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "error() called: vc=" + vc + " ioe=" + ioe);
        }
        HttpOutboundServiceContextImpl mySC = (HttpOutboundServiceContextImpl) vc.getStateMap().get(CallbackIDs.CALLBACK_HTTPOSC);
        mySC.setPersistent(false);
        mySC.reConnect(vc, ioe);
    }
}
