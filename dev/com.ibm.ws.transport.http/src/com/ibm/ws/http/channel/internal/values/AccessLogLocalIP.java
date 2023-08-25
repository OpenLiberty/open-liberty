/*******************************************************************************
 * Copyright (c) 2004, 2008 IBM Corporation and others.
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

public class AccessLogLocalIP extends AccessLogData {

    public AccessLogLocalIP() {
        super("%A");
        // %A - Local IP-address
    }

    @Override
    public boolean set(StringBuilder accessLogEntry,
                       HttpResponseMessage response, HttpRequestMessage request,
                       Object data) {
        String localIPAddress = getLocalIP(response, request, data);

        if (localIPAddress != null) {
            accessLogEntry.append(localIPAddress);
        } else {
            accessLogEntry.append("-");
        }
        return Boolean.TRUE;
    }

    public static String getLocalIP(HttpResponseMessage response, HttpRequestMessage request, Object data) {

        String localIPAddress = null;
        if (Objects.nonNull(request) && Objects.nonNull(request.getServiceContext())) {
            localIPAddress = request.getServiceContext().getLocalAddr().toString();
            localIPAddress = localIPAddress.substring(localIPAddress.indexOf('/') + 1);
        }

        return localIPAddress;
    }

}
