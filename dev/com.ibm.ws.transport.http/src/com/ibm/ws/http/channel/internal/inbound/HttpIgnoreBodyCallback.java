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

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.http.channel.internal.CallbackIDs;
import com.ibm.ws.http.channel.internal.HttpMessages;
import com.ibm.wsspi.bytebuffer.WsByteBuffer;
import com.ibm.wsspi.channelfw.InterChannelCallback;
import com.ibm.wsspi.channelfw.VirtualConnection;

/**
 * In certain scenarios, the channel needs to read and discard the incoming
 * request body. If any error occurs while doing that, then the connection can
 * be closed from here.
 * 
 */
public class HttpIgnoreBodyCallback implements InterChannelCallback {
    /** RAS tracing variable */
    private static final TraceComponent tc = Tr.register(HttpIgnoreBodyCallback.class, HttpMessages.HTTP_TRACE_NAME, HttpMessages.HTTP_BUNDLE);

    /** Singleton object */
    private static HttpIgnoreBodyCallback myInstance = null;

    /**
     * Private constructor, use the getRef() method
     */
    private HttpIgnoreBodyCallback() {
        // nothing to do
    }

    /**
     * Create the singleton instance of the class here
     */
    static private synchronized void createSingleton() {
        if (null == myInstance) {
            myInstance = new HttpIgnoreBodyCallback();
        }
    }

    /**
     * Get access to the singleton instance of this class
     * 
     * @return HttpIgnoreBodyCallback
     */
    public static final HttpIgnoreBodyCallback getRef() {
        if (null == myInstance) {
            createSingleton();
        }
        return myInstance;
    }

    /*
     * @see
     * com.ibm.wsspi.channelfw.InterChannelCallback#complete(com.ibm.wsspi.channelfw
     * .VirtualConnection)
     */
    public void complete(VirtualConnection vc) {

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "complete() called: " + vc);
        }
        // can't do anything with a null VC
        if (null == vc) {
            return;
        }
        Object o = vc.getStateMap().get(CallbackIDs.CALLBACK_HTTPISC);
        if (null == o) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "ERROR: null ISC in complete()");
            }
            return;
        }
        HttpInboundServiceContextImpl sc = (HttpInboundServiceContextImpl) o;
        // need to continue the purge of the request body
        try {
            VirtualConnection rc = null;
            do {
                WsByteBuffer buffer = sc.getRequestBodyBuffer();
                if (null != buffer) {
                    buffer.release();
                    rc = sc.getRequestBodyBuffer(this, false);
                } else {
                    // end of body found
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Reached end of the body being purged");
                    }
                    sc.getLink().close(vc, null);
                    return;
                }
            } while (null != rc);
        } catch (Exception purgeException) {
            // no FFDC required
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Exception purging request body: " + purgeException);
            }
            sc.getLink().close(vc, purgeException);
        }
        // if we get here then we're waiting for the next callback usage
    }

    /*
     * @see
     * com.ibm.wsspi.channelfw.InterChannelCallback#error(com.ibm.wsspi.channelfw
     * .VirtualConnection, java.lang.Throwable)
     */
    public void error(VirtualConnection vc, Throwable t) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "error() called: " + vc);
        }
        // can't do anything with a null VC
        if (null == vc) {
            return;
        }
        Object o = vc.getStateMap().get(CallbackIDs.CALLBACK_HTTPICL);
        if (null == o) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "ERROR: null ICL in error()");
            }
            return;
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Error occurring while purging body: " + t);
        }
        // close the connection
        ((HttpInboundLink) o).close(vc, (Exception) t);
    }

}