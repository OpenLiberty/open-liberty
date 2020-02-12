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

import com.ibm.ws.http.channel.internal.HttpResponseMessageImpl;
import com.ibm.wsspi.http.channel.HttpRequestMessage;
import com.ibm.wsspi.http.channel.HttpResponseMessage;

public class AccessLogResponseSize extends AccessLogData {

    public AccessLogResponseSize() {
        super("%b");
        // %b
        // Size of response in bytes, excluding HTTP headers.
        // In CLF format, i.e. a '-' rather than a 0 when no bytes are sent.
    }

    @Override
    public boolean set(StringBuilder accessLogEntry,
                       HttpResponseMessage response, HttpRequestMessage request, Object data) {

        long responseSize = getResponseSize(response, request, data);

        if (responseSize > 0) {
            accessLogEntry.append(responseSize);
        } else {
            accessLogEntry.append("-");
        }

        return true;
    }

    public static long getResponseSize(HttpResponseMessage response, HttpRequestMessage request, Object data) {
        long responseSize = -999;
        HttpResponseMessageImpl responseMessageImpl = null;
        if (response != null) {
            responseMessageImpl = (HttpResponseMessageImpl) response;
        }

        if (responseMessageImpl != null) {

            responseSize = responseMessageImpl.getServiceContext().getNumBytesWritten();

        }
        return responseSize;
    }

    public static String getResponseSizeAsString(HttpResponseMessage response, HttpRequestMessage request, Object data) {
        long responseSize = getResponseSize(response, request, data);
        if (responseSize > 0) {
            return Long.toString(responseSize);
        }
        // Return null instead of 0, since %B already returns 0 (%b will return it formatted as string)
        return null;
    }
}
