/*******************************************************************************
 * Copyright (c) 1997, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer31.upgrade;

import java.io.IOException;
import java.util.Enumeration;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.http2.upgrade.H2UpgradeHandler;
import com.ibm.ws.webcontainer.servlet.H2Handler;
import com.ibm.wsspi.http.HttpInboundConnection;
import com.ibm.wsspi.http.ee8.Http2InboundConnection;

/**
 * Implementation of the H2Handler interface
 */
public class H2HandlerImpl implements H2Handler {

    private static final TraceComponent tc = Tr.register(H2HandlerImpl.class,null);
    final static String CONSTANT_upgrade = new String("upgrade");
    final static String CONSTANT_connection = new String("connection");

    /**
     * Determines if a given request is an http2 upgrade request
     */
    @Override
    public boolean isH2Request(HttpInboundConnection hic, ServletRequest request) throws ServletException {
        
        //first check if H2 is enabled for this channel/port
        // Retrieve the needed header values for this request

        Enumeration<String> connection = ((HttpServletRequest) request).getHeaders(CONSTANT_connection);
        Enumeration<String> upgrade = ((HttpServletRequest) request).getHeaders(CONSTANT_upgrade);
        return ((Http2InboundConnection)hic).isHTTP2UpgradeRequest(connection, upgrade, true);
    }

    /**
     * Upgrades the given request for http2
     */
    @Override
    public void handleRequest(HttpInboundConnection hic, HttpServletRequest request, HttpServletResponse response) {
        Http2InboundConnection h2ic = (Http2InboundConnection) hic;
        H2UpgradeHandlerWrapper h2uh = null;
        try {
            h2uh = request.upgrade(H2UpgradeHandlerWrapper.class);
        } catch (IOException e) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "returning: user configurator threw an IOException. Exception message: " + e.getMessage());
            }
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;

        } catch (ServletException e) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "returning: user configurator threw a ServletException. Exception message: " + e.getMessage());
            }
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }
        if (h2uh != null) {
            h2uh.init(new H2UpgradeHandler());
        }
        
        HttpServletRequest hsrt = (HttpServletRequest) request;
        Enumeration<String> headerNames = hsrt.getHeaderNames();

        String http2Settings = null;
        while (headerNames.hasMoreElements()) {
            String key = headerNames.nextElement();
            if ("HTTP2-Settings".equals(key)) {
                http2Settings = hsrt.getHeader(key);
                break;
            }
        }
        boolean upgraded = h2ic.handleHTTP2UpgradeRequest(http2Settings);
        if (!upgraded) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "returning: http2 connection initialization failed");
            }
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

}
