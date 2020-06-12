/*******************************************************************************
 * Copyright (c) 2004, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.http.channel.internal.values;

import com.ibm.ws.http.channel.internal.HttpRequestMessageImpl;
import com.ibm.ws.http.channel.internal.inbound.HttpInboundServiceContextImpl;
import com.ibm.wsspi.http.channel.HttpRequestMessage;
import com.ibm.wsspi.http.channel.HttpResponseMessage;

public class AccessLogRemoteIP extends AccessLogData {

    public AccessLogRemoteIP() {
        super("%a");
        // %a
        // Remote IP-address
    }

    @Override
    public boolean set(StringBuilder accessLogEntry,
                       HttpResponseMessage response, HttpRequestMessage request,
                       Object data) {
        HttpRequestMessageImpl requestMessageImpl = null;
        String hostIPAddress = null;
        if (request != null) {
            requestMessageImpl = (HttpRequestMessageImpl) request;
        }

        if (requestMessageImpl != null) {

            hostIPAddress = null;

            if (requestMessageImpl.getServiceContext() instanceof HttpInboundServiceContextImpl) {
                HttpInboundServiceContextImpl serviceContext = (HttpInboundServiceContextImpl) requestMessageImpl.getServiceContext();
                if (serviceContext.useForwardedHeadersInAccessLog()) {
                    hostIPAddress = serviceContext.getForwardedRemoteAddress();
                }
            }

            if (hostIPAddress == null) {

                hostIPAddress = requestMessageImpl.getServiceContext().getRemoteAddr().toString();
                hostIPAddress = hostIPAddress.substring(hostIPAddress.indexOf('/') + 1);
            }
        }

        if (hostIPAddress != null) {
            accessLogEntry.append(hostIPAddress);
        } else {
            accessLogEntry.append("-");
        }
        return true;
    }
}
