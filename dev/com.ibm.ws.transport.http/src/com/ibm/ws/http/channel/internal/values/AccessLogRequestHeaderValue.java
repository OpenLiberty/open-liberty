/*******************************************************************************
 * Copyright (c) 2004, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.http.channel.internal.values;

import com.ibm.wsspi.http.channel.HttpRequestMessage;
import com.ibm.wsspi.http.channel.HttpResponseMessage;

public class AccessLogRequestHeaderValue extends AccessLogData {

    public AccessLogRequestHeaderValue() {
        super("%i");
        //%{HeaderName}i
        // The contents of HeaderLine: header line(s) in the request sent to the server.
    }

    @Override
    public Object init(String rawToken) {
        // return the what is the header name
        if (rawToken != null && rawToken.length() == 0) {
            return null;
        }
        return rawToken;
    }

    @Override
    public boolean set(StringBuilder accessLogEntry,
                       HttpResponseMessage response, HttpRequestMessage request, Object data) {
        String headerName = (String) data;
        String headerValue = null;

        if (headerName != null) {
            headerValue = getHeaderValue(response, request, data);
        }

        if (headerValue != null) {
            accessLogEntry.append(headerValue);
        } else {
            accessLogEntry.append("-");
        }

        return true;
    }

    public static String getHeaderValue(HttpResponseMessage response, HttpRequestMessage request, Object data) {
        String headerName = (String) data;
        String headerValue = null;
        if (headerName != null) {
            headerValue = request.getHeader(headerName).asString();
        }
        // A null header value will not show up in JSON log
        return headerValue;
    }

}
