/*******************************************************************************
 * Copyright (c) 2004, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *
 * PH49305 LLA 09/29/22 - Allow header multiples
 *******************************************************************************/
package com.ibm.ws.http.channel.internal.values;

import java.util.Iterator;

import com.ibm.wsspi.genericbnf.HeaderField;
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

        if (headerName != null) {
            // Some headers are allowed to have multiples such as X-Forwarded-For, get them all
            int count = request.getNumberOfHeaderInstances(headerName);

            if (0 == count) {
                accessLogEntry.append("-");
            } else {
                Iterator<HeaderField> it = request.getHeaders(headerName).iterator();
                accessLogEntry.append(it.next().asString());
                while (it.hasNext()) {
                    accessLogEntry.append(", ");
                    accessLogEntry.append(it.next().asString());
                }
            }

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
