/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
/*
 * Some of the code was derived from code supplied by the Apache Software Foundation licensed under the Apache License, Version 2.0.
 */
package com.ibm.ws.transport.iiop.security.config.ssl.yoko;

import java.net.Socket;

import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;

import org.apache.yoko.orb.OCI.IIOP.TransportInfo_impl;
import org.apache.yoko.orb.PortableInterceptor.ServerRequestInfoExt;
import org.omg.CORBA.LocalObject;
import org.omg.PortableInterceptor.ServerRequestInfo;
import org.omg.PortableInterceptor.ServerRequestInterceptor;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.transport.iiop.security.config.ssl.SSLSessionManager;

/**
 * A service context interceptor to help manage
 * SSL security information for incoming connections.
 *
 * @version $Revision: 452600 $ $Date: 2006-10-03 12:29:42 -0700 (Tue, 03 Oct 2006) $
 */
final class ServiceContextInterceptor extends LocalObject implements ServerRequestInterceptor {

    private static final TraceComponent tc = Tr.register(ServiceContextInterceptor.class);

    public ServiceContextInterceptor() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
            Tr.debug(tc, "<init>");
    }

    @Override
    public void receive_request(ServerRequestInfo ri) {}

    @Override
    public void receive_request_service_contexts(ServerRequestInfo ri) {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
            Tr.debug(tc, "Looking for SSL Session");

        // for an incoming request, we need to see if the request is coming in on
        // an SSLSocket.  If this is using a secure connection, then we register the
        // request and SSLSession with the session manager.
        ServerRequestInfoExt riExt = (ServerRequestInfoExt) ri;
        TransportInfo_impl connection = (TransportInfo_impl) riExt.getTransportInfo();
        if (connection != null) {
            Socket socket = connection.getSocket();
            if (socket != null && socket instanceof SSLSocket) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                    Tr.debug(tc, "Found SSL Session");
                SSLSocket sslSocket = (SSLSocket) socket;

                SSLSessionManager.setSSLSession(ri.request_id(), sslSocket.getSession());
            }
        }
    }

    @Override
    public void send_exception(ServerRequestInfo ri) {
        // clean any SSL session information if we registered.
        SSLSession old = SSLSessionManager.clearSSLSession(ri.request_id());
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled() && old != null)
            Tr.debug(tc, "Removing SSL Session for send_exception");
    }

    @Override
    public void send_other(ServerRequestInfo ri) {
        // clean any SSL session information if we registered.
        SSLSession old = SSLSessionManager.clearSSLSession(ri.request_id());
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled() && old != null)
            Tr.debug(tc, "Removing SSL Session for send_reply");
    }

    @Override
    public void send_reply(ServerRequestInfo ri) {
        // clean any SSL session information if we registered.
        SSLSession old = SSLSessionManager.clearSSLSession(ri.request_id());
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled() && old != null)
            Tr.debug(tc, "Removing SSL Session for send_reply");
    }

    @Override
    public void destroy() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
            Tr.debug(tc, "Destroy");
    }

    @Override
    public String name() {
        return "org.apache.geronimo.yoko.ServiceContextInterceptor";
    }
}
