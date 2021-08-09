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
package com.ibm.ws.http.channel.internal.inbound;

import java.io.IOException;
import java.net.SocketTimeoutException;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.http.channel.internal.CallbackIDs;
import com.ibm.ws.http.channel.internal.HttpMessages;
import com.ibm.wsspi.channelfw.VirtualConnection;
import com.ibm.wsspi.genericbnf.exception.MessageSentException;
import com.ibm.wsspi.http.channel.values.StatusCodes;
import com.ibm.wsspi.http.logging.DebugLog;
import com.ibm.wsspi.tcpchannel.TCPReadCompletedCallback;
import com.ibm.wsspi.tcpchannel.TCPReadRequestContext;

/**
 * Callback class used while parsing the inbound request message and a read
 * is necessary for more data, either the first line or the request headers.
 */
public class HttpICLReadCallback implements TCPReadCompletedCallback {

    /** RAS tracing variable */
    private static final TraceComponent tc = Tr.register(HttpICLReadCallback.class, HttpMessages.HTTP_TRACE_NAME, HttpMessages.HTTP_BUNDLE);

    /** Singleton instance of the class */
    private static final HttpICLReadCallback myInstance = new HttpICLReadCallback();

    /**
     * Private constructor, use the getRef() API for access.
     */
    private HttpICLReadCallback() {
        // nothing to do
    }

    /**
     * Get access to the singleton instance of the class.
     * 
     * @return HttpICLReadCallback
     */
    public static final HttpICLReadCallback getRef() {
        return myInstance;
    }

    /**
     * Called by the device side channel when the read has completed.
     * 
     * @param vc
     * @param rsc
     */
    @Override
    public void complete(VirtualConnection vc, TCPReadRequestContext rsc) {

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "complete() called: " + vc);
        }
        HttpInboundLink myLink = (HttpInboundLink) vc.getStateMap().get(CallbackIDs.CALLBACK_HTTPICL);

        myLink.processRequest();
    }

    /**
     * Called by the device side channel when an error occurred.
     * 
     * @param vc
     * @param rsc
     * @param ioe
     */
    @Override
    public void error(VirtualConnection vc, TCPReadRequestContext rsc, IOException ioe) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "error() called: " + vc);
        }

        HttpInboundLink myLink = (HttpInboundLink) (vc == null ? null : vc.getStateMap().get(CallbackIDs.CALLBACK_HTTPICL));
        if (myLink == null)
        {

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "error: myLink is null");

            }

        }

        else {

            HttpInboundServiceContextImpl isc = myLink.getHTTPContext();
            isc.setPersistent(false);
            myLink.setFilterCloseExceptions(true);
            // if there was no data in the 2nd or later request, then just close
            // as this would be a timeout on the persist read. All other cases
            // we should send the 408 Request Timeout error page back
            if ((myLink.isFirstRequest() || myLink.isPartiallyParsed())
                && (ioe instanceof SocketTimeoutException)) {
                try {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Error, sending 408 timeout back");
                    }
                    if (isc.getHttpConfig().getDebugLog().isEnabled(DebugLog.Level.WARN)) {
                        isc.getHttpConfig().getDebugLog().log(DebugLog.Level.WARN, HttpMessages.MSG_READ_FAIL, isc);
                    }
                    isc.setHeadersParsed();
                    isc.sendError(StatusCodes.REQ_TIMEOUT.getHttpError());
                } catch (MessageSentException mse) {
                    FFDCFilter.processException(mse, getClass().getName() + ".error", "152");
                    // shouldn't be possible since we're timing out during the
                    // read for the inbound request
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "MessageSent error during ICL read error");
                    }
                    myLink.close(vc, ioe);
                }
            } else {
                // this is during the read for a secondary request message with no
                // current parse taking place.
                myLink.close(vc, ioe);
            }
        }
    }
}
