/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package io.openliberty.webcontainer60.srt;

import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.servlet.request.IRequest;
import com.ibm.ws.webcontainer40.srt.SRTServletRequest40;
import com.ibm.wsspi.webcontainer.logging.LoggerFactory;

import io.openliberty.webcontainer60.osgi.srt.SRTConnectionContext60;
import io.openliberty.websphere.servlet60.IRequest60;
import jakarta.servlet.ServletConnection;
import jakarta.servlet.http.HttpServletRequest;

public class SRTServletRequest60 extends SRTServletRequest40 implements HttpServletRequest {

    protected static final Logger logger = LoggerFactory.getInstance().getLogger("io.openliberty.webcontainer60.srt");
    private static final String CLASS_NAME = SRTServletRequest60.class.getName();

    private static long startID = 1;
    private static AtomicLong counter = new AtomicLong(startID);

    public SRTServletRequest60(SRTConnectionContext60 context) {
        super(context);
    }

    @Override
    public void initForNextRequest(IRequest req) {
        String methodName = "initForNextRequest";

        super.initForNextRequest(req);

        if (req != null) {
            super.setSrtRequestId(String.valueOf(counter.getAndIncrement()));

            this.initForServletConnection();

            if (TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
                logger.logp(Level.FINE, CLASS_NAME, methodName,
                            "this [" + this + "] , request id [" + this.getRequestId() + "] , connection id ["
                                                                + ((SRTServletConnection) super.getSrtServletConnection()).getConnectionId() + "] , req [" + req + "]");
            }
        }
    }

    private void initForServletConnection() {
        SRTServletConnection servletConn = new SRTServletConnection();

        servletConn.setConnectionID(String.valueOf(((IRequest60) _request).getConnectionId()));
        servletConn.setConnectionSecure(super._request.isSSL());
        servletConn.setProtocol(super._request.getProtocol());

        super.setSrtServletConnection(servletConn);
    }

    /*
     * since: servlet 6.0
     * https://jakarta.ee/specifications/servlet/6.0/apidocs/jakarta.servlet/jakarta/servlet/servletrequest#getRequestId()
     */
    @Override
    public String getRequestId() {
        String id = super.getSrtRequestId();
        if (TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
            logger.logp(Level.FINE, CLASS_NAME, "getRequestId", "this [" + this + "] , request id [" + id + "]");
        }
        return id;
    }

    /*
     * since: servlet 6.0
     * https://jakarta.ee/specifications/servlet/6.0/apidocs/jakarta.servlet/jakarta/servlet/servletrequest#getProtocolRequestId()
     *
     * Returns: Stream id for HTTP 2; Empty string for other (including HTTP 3 as it is not supported yet)
     */
    @Override
    public String getProtocolRequestId() {
        //Do not cache this protocol id as upgrade can happen
        String streamId = "";
        int id = ((IRequest60) _request).getStreamId();
        if (id >= 0) {
            streamId = String.valueOf(id);
        }

        if (TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
            logger.logp(Level.FINE, CLASS_NAME, "getProtocolRequestId", "this [" + this + "] , returns stream id [" + streamId + "]");
        }

        return streamId;
    }

    /*
     * since: servlet 6.0
     * https://jakarta.ee/specifications/servlet/6.0/apidocs/jakarta.servlet/jakarta/servlet/servletrequest#getServletConnection()
     */
    @Override
    public ServletConnection getServletConnection() {
        if (TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
            logger.logp(Level.FINE, CLASS_NAME, "getServletConnection", "this [" + this + "]");
        }

        return (ServletConnection) super.getSrtServletConnection();
    }
}
