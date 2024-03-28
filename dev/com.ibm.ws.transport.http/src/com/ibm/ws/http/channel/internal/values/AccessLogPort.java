/*******************************************************************************
 * Copyright (c) 2004, 2021 IBM Corporation and others.
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

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.http.channel.internal.HttpMessages;
import com.ibm.wsspi.http.channel.HttpRequestMessage;
import com.ibm.wsspi.http.channel.HttpResponseMessage;

public class AccessLogPort extends AccessLogData {

    /** Trace component for debugging */
    private static final TraceComponent tc = Tr.register(AccessLogPort.class, HttpMessages.HTTP_TRACE_NAME, HttpMessages.HTTP_BUNDLE);

    public static final String TYPE_REMOTE = "remote";

    public AccessLogPort() {
        super("%p");
        // %p - Local port
        // %{remote}p - Remote port
    }

    @Override
    public Object init(String rawToken) {
        if (rawToken != null && rawToken.length() == 0) {
            return null;
        }
        return rawToken;
    }

    @Override
    public boolean set(StringBuilder accessLogEntry,
                       HttpResponseMessage response, HttpRequestMessage request,
                       Object data) {
        logSafe(accessLogEntry, getPort(response, request, data));
        return Boolean.TRUE;
    }

    public static String getPort(HttpResponseMessage response, HttpRequestMessage request, Object data) {
        // Matching logic in multiple places in com.ibm.ws.http.logging.source.AccessLogSource
        if (TYPE_REMOTE.equals(data)) {
            // %{remote}p
            return getRemotePort(response, request, data);
        } else {
            // %p
            return getLocalPort(response, request, data);
        }
    }

    public static String getLocalPort(HttpResponseMessage response, HttpRequestMessage request, Object data) {

        String localPort = null;

        if (Objects.nonNull(request)) {
            localPort = Objects.nonNull(request.getServiceContext()) ? Integer.toString(request.getServiceContext().getLocalPort()) : null;
        }

        return localPort;
    }

    public static String getRemotePort(HttpResponseMessage response, HttpRequestMessage request, Object data) {

        String remotePort = null;
        if (Objects.nonNull(request)) {
            remotePort = Objects.nonNull(request.getServiceContext()) ? Integer.toString(request.getServiceContext().getRemotePort()) : null;
        }

        return remotePort;
    }
}
