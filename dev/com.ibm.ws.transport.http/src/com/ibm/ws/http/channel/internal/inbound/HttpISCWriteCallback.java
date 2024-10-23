/*******************************************************************************
 * Copyright (c) 2004, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
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
import com.ibm.ws.http.dispatcher.internal.HttpDispatcher;
import com.ibm.wsspi.channelfw.VirtualConnection;
import com.ibm.wsspi.http.channel.exception.HttpInvalidMessageException;
import com.ibm.wsspi.http.logging.DebugLog;
import com.ibm.wsspi.tcpchannel.TCPWriteCompletedCallback;
import com.ibm.wsspi.tcpchannel.TCPWriteRequestContext;

/**
 * Callback class used while writing the outgoing response message on a
 * particular connection.
 */
public class HttpISCWriteCallback implements TCPWriteCompletedCallback {

    /** RAS tracing variable */
    private static final TraceComponent tc = Tr.register(HttpISCWriteCallback.class, HttpMessages.HTTP_TRACE_NAME, HttpMessages.HTTP_BUNDLE);

    /** Singleton object */
    private static final HttpISCWriteCallback myInstance = new HttpISCWriteCallback();

    /**
     * Private constructor, use the getRef() method.
     */
    private HttpISCWriteCallback() {
        // nothing to do
    }

    /**
     * Get access to the singleton instance of this class.
     * 
     * @return HttpISCWriteCallback
     */
    public static final HttpISCWriteCallback getRef() {
        return myInstance;
    }

    /**
     * Called by the channel below us when a write has finished.
     * 
     * @param vc
     * @param wsc
     */
    @Override
    public void complete(VirtualConnection vc, TCPWriteRequestContext wsc) {

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "complete() called: vc=" + vc);
        }
        HttpInboundServiceContextImpl mySC = (HttpInboundServiceContextImpl) vc.getStateMap().get(CallbackIDs.CALLBACK_HTTPISC);
        if (null != wsc) {
            wsc.setBuffers(null);
        }

        // access logging if the finishResponse API was used
        if (mySC.isMessageSent()) {
            mySC.logFinalResponse(mySC.getNumBytesWritten());
            HttpInvalidMessageException inv = mySC.checkResponseValidity();
            if (null != inv) {
                // response was invalid in some way... error scenario
                mySC.setPersistent(false);
                if (null != mySC.getAppWriteCallback()) {
                    mySC.getAppWriteCallback().error(vc, inv);
                }
                return;
            }
        }

        // if a callback exists above, pass along the complete. But if the
        // app channel didn't give us a callback (it doesn't care) then we
        // can't pass anything along and we stop further work here
        if (null != mySC.getAppWriteCallback()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Calling write complete callback of app channel.");
            }
            int status = mySC.getResponse().getStatusCodeAsInt();
            boolean isTemporaryStatus = HttpDispatcher.useEE7Streams() && status == 101 ? false: (100<=status && 200 > status);
                
            
            
            
            if (isTemporaryStatus) {
                // allow other responses to follow a temporary one
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Temp response sent, resetting send flags");
                }
                mySC.resetWrite();
            }
            mySC.getAppWriteCallback().complete(vc);
        } else {
            // this is dangerous since we're not notifying the channel above
            // of the complete work, but they don't care since they didn't
            // give us a callback to use... hopefully they will continue this
            // connection properly.
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "No available app channel callback");
            }
        }
    }

    /**
     * Called by the channel below us when an error occurs during a write.
     * 
     * @param vc
     *            - VirtualConnection
     * @param wsc
     *            - TCPWriteRequestConext
     * @param ioe
     */
    @Override
    public void error(VirtualConnection vc, TCPWriteRequestContext wsc, IOException ioe) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "error called: vc=" + vc + " ioe=" + ioe);
        }
        HttpInboundServiceContextImpl mySC = (HttpInboundServiceContextImpl) vc.getStateMap().get(CallbackIDs.CALLBACK_HTTPISC);

        if (mySC.getHttpConfig().getDebugLog().isEnabled(DebugLog.Level.WARN)) {
            mySC.getHttpConfig().getDebugLog().log(DebugLog.Level.WARN, HttpMessages.MSG_WRITE_FAIL, mySC);
        }
        mySC.logLegacyMessage();
        // pass on the error if possible, else just close the connection
        mySC.setPersistent(false);
        if (null != mySC.getAppWriteCallback()) {
            mySC.getAppWriteCallback().error(vc, ioe);
        } else {
            mySC.getLink().getDeviceLink().close(vc, ioe);
        }
    }
}
