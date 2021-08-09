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
import com.ibm.wsspi.tcpchannel.TCPReadCompletedCallback;
import com.ibm.wsspi.tcpchannel.TCPReadRequestContext;

/**
 * Callback class used while reading the inbound request body.
 * 
 */
public class HttpISCBodyReadCallback implements TCPReadCompletedCallback {
    /** RAS tracing variable */
    private static final TraceComponent tc = Tr.register(HttpISCBodyReadCallback.class, HttpMessages.HTTP_TRACE_NAME, HttpMessages.HTTP_BUNDLE);

    /** Singleton object */
    private static final HttpISCBodyReadCallback myInstance = new HttpISCBodyReadCallback();

    /**
     * Private constructor, use the getRef() method.
     */
    private HttpISCBodyReadCallback() {
        // nothing to do
    }

    /**
     * Get access to the singleton instance of this class.
     * 
     * @return HttpISCBodyReadCallback
     */
    public static final HttpISCBodyReadCallback getRef() {
        return myInstance;
    }

    /*
     * @see
     * com.ibm.wsspi.tcpchannel.TCPReadCompletedCallback#complete(com.ibm.wsspi
     * .channelfw.VirtualConnection,
     * com.ibm.wsspi.tcpchannel.TCPReadRequestContext)
     */
    @SuppressWarnings("unused")
    public void complete(VirtualConnection vc, TCPReadRequestContext rsc) {

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "complete() called: vc=" + vc);
        }
        HttpInboundServiceContextImpl mySC = (HttpInboundServiceContextImpl) vc.getStateMap().get(CallbackIDs.CALLBACK_HTTPISC);
        mySC.continueRead();
    }

    /*
     * @see
     * com.ibm.wsspi.tcpchannel.TCPReadCompletedCallback#error(com.ibm.wsspi.channelfw
     * .VirtualConnection, com.ibm.wsspi.tcpchannel.TCPReadRequestContext,
     * java.io.IOException)
     */
    @SuppressWarnings("unused")
    public void error(VirtualConnection vc, TCPReadRequestContext rsc, IOException ioe) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "error() called: vc=" + vc + " ioe=" + ioe);
        }
        HttpInboundServiceContextImpl mySC = (HttpInboundServiceContextImpl) vc.getStateMap().get(CallbackIDs.CALLBACK_HTTPISC);

        // request bodies cannot be delimited by the socket closing so this
        // exception means we're broken
        mySC.setPersistent(false);
        if (null != mySC.getAppReadCallback()) {
            mySC.getAppReadCallback().error(vc, ioe);
        } else {
            // no app callback above
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Error on the request body read but no app cb");
            }
        }
    }
}
