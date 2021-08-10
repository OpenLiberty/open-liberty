/*******************************************************************************
 * Copyright (c) 2004, 2008 IBM Corporation and others.
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
import com.ibm.wsspi.http.channel.HttpRequestMessage;
import com.ibm.wsspi.http.channel.HttpResponseMessage;

public class AccessLogRemotePort extends AccessLogData {

    public AccessLogRemotePort() {
        super("%e");
        // %e - Remote port
    }

    @Override
    public boolean set(StringBuilder accessLogEntry,
                       HttpResponseMessage response, HttpRequestMessage request,
                       Object data) {
        String remotePort = getRemotePort(response, request, data);

        logSafe(accessLogEntry, remotePort);
        return true;
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

}
