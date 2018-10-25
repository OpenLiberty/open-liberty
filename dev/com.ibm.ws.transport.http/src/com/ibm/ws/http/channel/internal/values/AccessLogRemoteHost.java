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

public class AccessLogRemoteHost extends AccessLogData {

    public AccessLogRemoteHost() {
        super("%h");
        // %h
        // Remote host
    }

    @Override
    public boolean set(StringBuilder accessLogEntry,
                       HttpResponseMessage response, HttpRequestMessage request, Object data) {
        HttpRequestMessageImpl requestMessageImpl = null;
        String hostAddress = null;
        if (request != null) {
            requestMessageImpl = (HttpRequestMessageImpl) request;
        }

        if (requestMessageImpl != null) {

            hostAddress = null;

            if (requestMessageImpl.getServiceContext() instanceof HttpInboundServiceContextImpl) {
                HttpInboundServiceContextImpl serviceContext = (HttpInboundServiceContextImpl) requestMessageImpl.getServiceContext();
                if (serviceContext.useForwardedHeadersInAccessLog()) {
                    hostAddress = serviceContext.getForwardedRemoteHost();
                }
            }

            if (hostAddress == null) {
                hostAddress = requestMessageImpl.getServiceContext().getRemoteAddr().getHostAddress();
            }

        }

        if (hostAddress != null) {
            accessLogEntry.append(hostAddress);
        } else {
            accessLogEntry.append("-");
        }
        return true;

//		String requestHost = null;
//		if(request != null){
//			requestHost = request.getURLHost();
//		}
//
//		if(requestHost != null){
//			accessLogEntry.append(requestHost);
//		} else {
//			accessLogEntry.append("-");
//		}
//
//		return true;
    }
}
