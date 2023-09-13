/*******************************************************************************
 * Copyright (c) 2004, 2020 IBM Corporation and others.
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
package com.ibm.ws.http.channel.internal.values;

import java.util.Objects;

import com.ibm.ws.http.netty.MSP;
import com.ibm.wsspi.http.channel.HttpRequestMessage;
import com.ibm.wsspi.http.channel.HttpResponseMessage;
import com.ibm.wsspi.http.channel.inbound.HttpInboundServiceContext;

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
        String hostIPAddress = getRemoteIP(response, request, data);

        if (Objects.nonNull(hostIPAddress)) {
            accessLogEntry.append(hostIPAddress);
        } else {
            accessLogEntry.append("-");
        }
        return Boolean.TRUE;
    }

    public static String getRemoteIP(HttpResponseMessage response, HttpRequestMessage request, Object data) {

        String hostIPAddress = null;

        if (Objects.nonNull(request)) {

            MSP.log("access log parsing %a");

            HttpInboundServiceContext serviceContext = request.getServiceContext() instanceof HttpInboundServiceContext ? (HttpInboundServiceContext) request.getServiceContext() : null;

            if (Objects.nonNull(serviceContext)) {
                hostIPAddress = serviceContext.useForwardedHeadersInAccessLog() ? serviceContext.getForwardedRemoteAddress() : null;

                MSP.log("Using forwarded " + serviceContext.useForwardedHeadersInAccessLog());
                MSP.log("Set host to : " + hostIPAddress);

                if (Objects.isNull(hostIPAddress)) {
                    hostIPAddress = serviceContext.getRemoteAddr().toString();
                    hostIPAddress = hostIPAddress.substring(hostIPAddress.indexOf('/') + 1);
                }
            }
        }

        return hostIPAddress;
    }
}
