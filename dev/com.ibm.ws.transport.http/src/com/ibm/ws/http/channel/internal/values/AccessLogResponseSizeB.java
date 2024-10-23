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

import io.openliberty.http.constants.HttpGenerics;

public class AccessLogResponseSizeB extends AccessLogData {

    public AccessLogResponseSizeB() {
        super("%B");
        // %B - Size of response in bytes, excluding HTTP headers.
    }

    @Override
    public boolean set(StringBuilder accessLogEntry,
                       HttpResponseMessage response, HttpRequestMessage request,
                       Object data) {

        long responseSize = getBytesReceived(response, request, data);

        if (responseSize != HttpGenerics.NOT_SET) {
            accessLogEntry.append(responseSize);
        } else {
            accessLogEntry.append("0");
        }

        return Boolean.TRUE;
    }

    public static long getBytesReceived(HttpResponseMessage response, HttpRequestMessage request, Object data) {

        return Objects.nonNull(response) ? response.getBytesWritten() : HttpGenerics.NOT_SET;

    }
}
