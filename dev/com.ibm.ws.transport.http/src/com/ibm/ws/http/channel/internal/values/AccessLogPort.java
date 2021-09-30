/*******************************************************************************
 * Copyright (c) 2004, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.http.channel.internal.values;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.http.channel.internal.HttpMessages;
import com.ibm.ws.http.channel.internal.HttpRequestMessageImpl;
import com.ibm.ws.kernel.productinfo.ProductInfo;
import com.ibm.wsspi.http.channel.HttpRequestMessage;
import com.ibm.wsspi.http.channel.HttpResponseMessage;

public class AccessLogPort extends AccessLogData {

    /** Trace component for debugging */
    private static final TraceComponent tc = Tr.register(AccessLogPort.class, HttpMessages.HTTP_TRACE_NAME, HttpMessages.HTTP_BUNDLE);

    @Deprecated
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
        return true;
    }

    public static String getPort(HttpResponseMessage response, HttpRequestMessage request, Object data) {
        // Matching logic in multiple places in com.ibm.ws.http.logging.source.AccessLogSource
        if (TYPE_REMOTE.equals(data)) {
            // %{remote}p
            betaFenceCheck();
            return getRemotePort(response, request, data);
        } else {
            // %p
            return getLocalPort(response, request, data);
        }
    }

    public static String getLocalPort(HttpResponseMessage response, HttpRequestMessage request, Object data) {
        HttpRequestMessageImpl requestMessageImpl = null;
        String localPort = null;
        if (request != null) {
            requestMessageImpl = (HttpRequestMessageImpl) request;
        }

        if (requestMessageImpl != null) {
            localPort = Integer.toString(requestMessageImpl.getServiceContext().getLocalPort());
        }
        return localPort;
    }

    public static String getRemotePort(HttpResponseMessage response, HttpRequestMessage request, Object data) {
        HttpRequestMessageImpl requestMessageImpl = null;
        String remotePort = null;
        if (request != null) {
            requestMessageImpl = (HttpRequestMessageImpl) request;
        }

        if (requestMessageImpl != null) {
            remotePort = Integer.toString(requestMessageImpl.getServiceContext().getRemotePort());
        }
        return remotePort;
    }

    // Flag tells us if the message for a call to a beta method has been issued
    private static boolean issuedBetaMessage = false;

    public static void betaFenceCheck() throws UnsupportedOperationException {
        // Not running beta edition, throw exception
        if (!ProductInfo.getBetaEdition()) {
            throw new UnsupportedOperationException("This method is beta and is not available.");
        } else {
            // Running beta exception, issue message if we haven't already issued one for this class
            if (!issuedBetaMessage) {
                Tr.info(tc, "BETA: A beta method has been invoked for the class " + AccessLogPort.class.getName() + " for the first time.");
                issuedBetaMessage = !issuedBetaMessage;
            }
        }
    }
}
