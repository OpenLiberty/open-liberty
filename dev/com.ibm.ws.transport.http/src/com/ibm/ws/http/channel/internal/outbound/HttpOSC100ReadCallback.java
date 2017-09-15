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
import com.ibm.wsspi.http.channel.exception.ExpectationFailedException;
import com.ibm.wsspi.http.channel.values.StatusCodes;
import com.ibm.wsspi.tcpchannel.TCPReadCompletedCallback;
import com.ibm.wsspi.tcpchannel.TCPReadRequestContext;

/**
 * Read callback used specific for when the outbound service context is reading
 * for the 100-continue response from the target server.
 */
public class HttpOSC100ReadCallback implements TCPReadCompletedCallback {

    /** RAS tracing variable */
    private static final TraceComponent tc = Tr.register(HttpOSC100ReadCallback.class, HttpMessages.HTTP_TRACE_NAME, HttpMessages.HTTP_BUNDLE);

    /** Singleton instance of the class */
    private static final HttpOSC100ReadCallback myInstance = new HttpOSC100ReadCallback();

    /**
     * Private constructor, use the getRef() method.
     */
    private HttpOSC100ReadCallback() {
        // nothing to do
    }

    /**
     * Get access to the singleton instance of the class.
     * 
     * @return HttpOSC100ReadCallback
     */
    public static final HttpOSC100ReadCallback getRef() {
        return myInstance;
    }

    /**
     * Helper method to handle when new work is around and we need to parse it
     * and watch for errors/check for headersParsed being true.
     * 
     * @param sc
     * @param vc
     * @return boolean (have headers been fully parsed yet?)
     */
    private boolean handleNewData(HttpOutboundServiceContextImpl sc, VirtualConnection vc) {
        if (!sc.headersParsed()) {
            try {
                return sc.parseMessage();
            } catch (Exception e) {
                FFDCFilter.processException(e, getClass().getName() + ".handleNewData", "73", this);
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Caught exception: " + e.getMessage());
                }
                sc.setPersistent(false);
                sc.getAppWriteCallback().error(vc, e);
                return false;
            }
        }
        return true;
    }

    /**
     * When the response has been received, we need to parse the status line
     * and verify that it was the "100 Continue". If it was, then pass along
     * the notification to the application channel. If it was anything else,
     * then pass an error up to the application channel using the specific
     * Expectation-Failed exception.
     * 
     * @param vc
     * @param rsc
     */
    public void complete(VirtualConnection vc, TCPReadRequestContext rsc) {

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "complete() called: vc=" + vc);
        }
        HttpOutboundServiceContextImpl mySC = (HttpOutboundServiceContextImpl) vc.getStateMap().get(CallbackIDs.CALLBACK_HTTPOSC);

        // keep reading and handling new data until either we're done
        // parsing headers or until we're waiting on a read to finish
        // Note: parseMessage will allocate the read buffers
        boolean rc = false;
        while (!rc && null != vc) {
            rc = handleNewData(mySC, vc);
            // if we're not done parsing, then read more data
            if (!rc) {
                // read whatever is available
                vc = rsc.read(1, this, false, mySC.getReadTimeout());
            }
        }
        // if rc is false, then this callback will be used later on when
        // the read completes, otherwise check the status code from the
        // response message
        if (rc) {
            StatusCodes status = mySC.getResponse().getStatusCode();
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "100-continue scenario received " + status);
            }
            if (status.equals(StatusCodes.CONTINUE)) {
                // got the 100-continue
                mySC.resetRead();
                mySC.getAppWriteCallback().complete(vc);
            } else {
                // anything else, pass along the ExpectationFailedException
                mySC.setPersistent(false);
                mySC.getAppWriteCallback().error(vc, new ExpectationFailedException(status.getIntCode() + " " + mySC.getResponse().getReasonPhrase()));
            }
        }
    }

    /**
     * Triggered when an error occurs during the read.
     * 
     * @param vc
     * @param rsc
     * @param ioe
     */
    public void error(VirtualConnection vc, TCPReadRequestContext rsc, IOException ioe) {

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "error() called: vc=" + vc + " ioe=" + ioe);
        }
        HttpOutboundServiceContextImpl mySC = (HttpOutboundServiceContextImpl) vc.getStateMap().get(CallbackIDs.CALLBACK_HTTPOSC);
        mySC.setPersistent(false);
        mySC.reConnect(vc, ioe);
    }

}
