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

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.http.channel.internal.CallbackIDs;
import com.ibm.ws.http.channel.internal.HttpMessages;
import com.ibm.wsspi.channelfw.VirtualConnection;
import com.ibm.wsspi.http.channel.HttpResponseMessage;
import com.ibm.wsspi.http.channel.values.HttpHeaderKeys;
import com.ibm.wsspi.http.channel.values.StatusCodes;
import com.ibm.wsspi.http.logging.DebugLog;
import com.ibm.wsspi.tcpchannel.TCPWriteCompletedCallback;
import com.ibm.wsspi.tcpchannel.TCPWriteRequestContext;

/**
 * Callback class used while writing the 100 continue response back to the
 * inbound client request.
 */
public class Http100ContWriteCallback implements TCPWriteCompletedCallback {

    /** RAS tracing variable */
    private static final TraceComponent tc = Tr.register(Http100ContWriteCallback.class, HttpMessages.HTTP_TRACE_NAME, HttpMessages.HTTP_BUNDLE);

    /** Singleton object */
    private static final Http100ContWriteCallback myInstance = new Http100ContWriteCallback();

    /**
     * Private constructor, use the getRef() method.
     */
    private Http100ContWriteCallback() {
        // nothing to do
    }

    /**
     * Get access to the singleton instance of this class.
     * 
     * @return Http100ContWriteCallback
     */
    public static final Http100ContWriteCallback getRef() {
        return myInstance;
    }

    /*
     * @see
     * com.ibm.wsspi.tcpchannel.TCPWriteCompletedCallback#complete(com.ibm.wsspi
     * .channelfw.VirtualConnection,
     * com.ibm.wsspi.tcpchannel.TCPWriteRequestContext)
     */
    @SuppressWarnings("unused")
    public void complete(VirtualConnection vc, TCPWriteRequestContext wsc) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "complete() called for vc=" + vc);
        }
        HttpInboundLink link = (HttpInboundLink) vc.getStateMap().get(CallbackIDs.CALLBACK_HTTPICL);
        // we've written the 100 continue response, now we can call up to the
        // app channels above us

        // reset the values on the response message first
        link.getHTTPContext().resetMsgSentState();
        HttpResponseMessage msg = link.getHTTPContext().getResponse();
        msg.setStatusCode(StatusCodes.OK);
        msg.removeHeader(HttpHeaderKeys.HDR_CONTENT_LENGTH);
        link.handleDiscrimination();
    }

    /*
     * @see
     * com.ibm.wsspi.tcpchannel.TCPWriteCompletedCallback#error(com.ibm.wsspi.
     * channelfw.VirtualConnection,
     * com.ibm.wsspi.tcpchannel.TCPWriteRequestContext, java.io.IOException)
     */
    @SuppressWarnings("unused")
    public void error(VirtualConnection vc, TCPWriteRequestContext wsc, IOException ioe) {

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "error() called for vc=" + vc + " ioe=" + ioe);
        }
        HttpInboundServiceContextImpl isc = (HttpInboundServiceContextImpl) vc.getStateMap().get(CallbackIDs.CALLBACK_HTTPISC);

        // log the failed response write
        if (isc.getHttpConfig().getDebugLog().isEnabled(DebugLog.Level.WARN)) {
            isc.getHttpConfig().getDebugLog().log(DebugLog.Level.WARN, HttpMessages.MSG_WRITE_FAIL, isc);
        }
        // close the connection now. No app channel has been involved at this
        // point yet.
        isc.getLink().close(vc, ioe);
    }

}