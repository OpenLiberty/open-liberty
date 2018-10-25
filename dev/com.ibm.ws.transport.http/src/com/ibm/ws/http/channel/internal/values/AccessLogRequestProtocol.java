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

public class AccessLogRequestProtocol extends AccessLogData {

    public AccessLogRequestProtocol() {
        super("%H");
    }

    @Override
    public boolean set(StringBuilder accessLogEntry,
                       HttpResponseMessage response, HttpRequestMessage request, Object data) {
        String protocol = null;
        HttpRequestMessageImpl requestMessageImpl = null;
        if (request != null) {

            requestMessageImpl = (HttpRequestMessageImpl) request;
        }

        if (requestMessageImpl != null) {

            if (requestMessageImpl.getServiceContext() instanceof HttpInboundServiceContextImpl) {
                HttpInboundServiceContextImpl serviceContext = (HttpInboundServiceContextImpl) requestMessageImpl.getServiceContext();
                if (serviceContext.useForwardedHeadersInAccessLog()) {
                    protocol = serviceContext.getForwardedRemoteProto();
                }
            }

            if (protocol == null) {
                protocol = request.getVersion();
            }

        }

        logSafe(accessLogEntry, protocol);

        return true;
    }

}
