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
 *******************************************************************************/
package com.ibm.ws.http.channel.internal.values;

import java.util.Iterator;

import com.ibm.wsspi.genericbnf.HeaderField;
import com.ibm.wsspi.http.channel.HttpRequestMessage;
import com.ibm.wsspi.http.channel.HttpResponseMessage;

public class AccessLogResponseHeaderValue extends AccessLogData {

    public AccessLogResponseHeaderValue() {
        super("%o");
        //%{HeaderName}o
        // The contents of HeaderName: header line(s) in the reply.
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
                       HttpResponseMessage response, HttpRequestMessage request, Object data) {
        String headerName = (String) data;

        if (headerName != null) {
            // Some headers are allowed to have multiples such as X-Forwarded-For, get them all
            int count = response.getNumberOfHeaderInstances(headerName);

            if (0 == count) {
                accessLogEntry.append("-");
            } else {
                Iterator<HeaderField> it = response.getHeaders(headerName).iterator();
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
            headerValue = response.getHeader(headerName).asString();
        }
        // Could be null - if it's null, don't print to JSON
        return headerValue;
    }
}
