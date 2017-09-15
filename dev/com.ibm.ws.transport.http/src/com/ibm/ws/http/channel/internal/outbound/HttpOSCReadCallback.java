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
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.http.channel.internal.CallbackIDs;
import com.ibm.ws.http.channel.internal.HttpMessages;
import com.ibm.wsspi.channelfw.VirtualConnection;
import com.ibm.wsspi.tcpchannel.TCPReadCompletedCallback;
import com.ibm.wsspi.tcpchannel.TCPReadRequestContext;

/**
 * Callback used while reading asynchronously for the response message first
 * line and headers.
 * 
 */
public class HttpOSCReadCallback implements TCPReadCompletedCallback {

    /** RAS tracing variable */
    private static final TraceComponent tc = Tr.register(HttpOSCReadCallback.class, HttpMessages.HTTP_TRACE_NAME, HttpMessages.HTTP_BUNDLE);

    /** Singleton instance of the class */
    private static final HttpOSCReadCallback myInstance = new HttpOSCReadCallback();

    /**
     * Private constructor, use the getRef() method.
     */
    private HttpOSCReadCallback() {
        // nothing to do
    }

    /**
     * Get access to the singleton instance of the class.
     * 
     * @return HttpOSCReadCallback
     */
    public static final HttpOSCReadCallback getRef() {
        return myInstance;
    }

    /**
     * Called by the channel below us when a read has completed.
     * 
     * @param vc
     * @param req
     */
    public void complete(VirtualConnection vc, TCPReadRequestContext req) {

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "complete() called: vc=" + vc);
        }
        HttpOutboundServiceContextImpl mySC = (HttpOutboundServiceContextImpl) vc.getStateMap().get(CallbackIDs.CALLBACK_HTTPOSC);
        // keep reading and handling new data until either we're done
        // parsing headers or until we're waiting on a read to finish
        VirtualConnection readVC = null;
        try {
            do {
                if (mySC.parseMessage()) {
                    // start processing the new parsed message
                    mySC.handleParsedMessage();
                    return;
                }
                // not done parsing, read for more data
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Reading for more data");
                }
                // configure the buffers for reading
                mySC.setupReadBuffers(mySC.getHttpConfig().getIncomingHdrBufferSize(), false);
                readVC = req.read(1, this, false, mySC.getReadTimeout());
            } while (null != readVC);
        } catch (Exception e) {
            FFDCFilter.processException(e, getClass().getName() + ".complete", "112", this);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Caught exception: " + e.getMessage());
            }
            mySC.setPersistent(false);
            mySC.getAppWriteCallback().error(vc, e);
        }
    }

    /**
     * Called by the channel below us when an error occurred during a read.
     * 
     * @param vc
     * @param req
     * @param ioe
     */
    public void error(VirtualConnection vc, TCPReadRequestContext req, IOException ioe) {

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "error() called: vc=" + vc + " ioe=" + ioe);
        }
        HttpOutboundServiceContextImpl mySC = (HttpOutboundServiceContextImpl) vc.getStateMap().get(CallbackIDs.CALLBACK_HTTPOSC);
        mySC.setPersistent(false);
        mySC.reConnect(vc, ioe);
    }
}
