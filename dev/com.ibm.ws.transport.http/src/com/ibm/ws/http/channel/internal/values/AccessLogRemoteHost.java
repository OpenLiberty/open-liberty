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

import com.ibm.wsspi.http.channel.HttpRequestMessage;
import com.ibm.wsspi.http.channel.HttpResponseMessage;
import com.ibm.wsspi.http.channel.inbound.HttpInboundServiceContext;

public class AccessLogRemoteHost extends AccessLogData {

    public AccessLogRemoteHost() {
        super("%h");
        // %h
        // Remote host
    }

    @Override
    public boolean set(StringBuilder accessLogEntry,
                       HttpResponseMessage response, HttpRequestMessage request, Object data) {
        String hostAddress = getRemoteHostAddress(response, request, data);

        if (hostAddress != null) {
            accessLogEntry.append(hostAddress);
        } else {
            accessLogEntry.append("-");
        }
        return Boolean.TRUE;
    }

    public static String getRemoteHostAddress(HttpResponseMessage response, HttpRequestMessage request, Object data) {

        String hostAddress = null;

        if (Objects.nonNull(request)) {

            HttpInboundServiceContext serviceContext = request.getServiceContext() instanceof HttpInboundServiceContext ? (HttpInboundServiceContext) request.getServiceContext() : null;

            if (Objects.nonNull(serviceContext)) {

                hostAddress = serviceContext.useForwardedHeadersInAccessLog() ? serviceContext.getForwardedRemoteHost() : null;

                if (Objects.isNull(hostAddress)) {
                    hostAddress = serviceContext.getRemoteAddr().getHostAddress();
                }
            }

        }

        return hostAddress;
    }
}
